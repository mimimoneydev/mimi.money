package com.money.mimi.fragments.home;

import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.money.mimi.R;
import com.money.mimi.adapters.recyclerView.agents.AiAgentAdapter;
import com.money.mimi.agents.CrooAgent;
import com.money.mimi.agents.CrooCataloguePage;
import com.money.mimi.agents.CrooCatalogueRepository;
import com.money.mimi.agents.CrooService;
import com.money.mimi.helpers.PreferenceManager;
import com.money.mimi.telemetry.AppTelemetry;

import java.util.List;

import okhttp3.Call;

/** Native, read-only CROO catalogue. Commercial actions continue in the secured wallet browser. */
public final class AiAgentMarketplaceFragment extends Fragment {
    private static final int PAGE_SIZE = 20;
    private static final String[] CATEGORY_TAGS = {"", "defi-trading", "data-analytics",
            "research-report", "development-code", "automation-workflow",
            "content-creative", "social-community"};
    private static final int[] CATEGORY_LABELS = {R.string.ai_agents_category_all,
            R.string.ai_agents_category_defi, R.string.ai_agents_category_data,
            R.string.ai_agents_category_research, R.string.ai_agents_category_development,
            R.string.ai_agents_category_automation, R.string.ai_agents_category_content,
            R.string.ai_agents_category_social};
    private static final long SEARCH_DEBOUNCE_MS = 450L;
    private static final int[] SERVICE_ACCENT_COLORS = {
            R.color.aiAgentServiceBlue, R.color.aiAgentServicePurple,
            R.color.aiAgentServiceTeal, R.color.aiAgentServiceOrange,
            R.color.aiAgentServicePink, R.color.aiAgentServiceIndigo
    };

    private CrooCatalogueRepository repository;
    private AiAgentAdapter adapter;
    private SwipeRefreshLayout refreshLayout;
    private RecyclerView list;
    private ProgressBar progress;
    private View state;
    private TextView stateText;
    private Button retry;
    private Button loadMore;
    private EditText search;
    private Button clearSearch;
    private RadioGroup categories;
    private TextView resultSummary;
    private final Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable pendingSearch;
    private Call activePageCall;
    private Call activeAgentCall;
    private AlertDialog activeDialog;
    private int pageGeneration;
    private int agentGeneration;
    private int currentPage;
    private int total;
    private int selectedCategoryIndex;
    private boolean loading;
    private AppTelemetry.OperationTrace pageLoadTrace;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_ai_agent_marketplace, container, false);
        AppTelemetry.logFeatureOpened("ai_agent_marketplace");
        repository = CrooCatalogueRepository.get(requireContext());
        adapter = new AiAgentAdapter(this::loadAgentDetails);
        refreshLayout = root.findViewById(R.id.ai_agents_refresh);
        list = root.findViewById(R.id.ai_agents_list);
        progress = root.findViewById(R.id.ai_agents_progress);
        state = root.findViewById(R.id.ai_agents_state);
        stateText = root.findViewById(R.id.ai_agents_state_text);
        retry = root.findViewById(R.id.ai_agents_retry);
        loadMore = root.findViewById(R.id.ai_agents_load_more);
        search = root.findViewById(R.id.ai_agents_search);
        clearSearch = root.findViewById(R.id.ai_agents_clear_button);
        categories = root.findViewById(R.id.ai_agents_categories);
        resultSummary = root.findViewById(R.id.ai_agents_result_summary);

        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);
        refreshLayout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimary);
        refreshLayout.setOnRefreshListener(() -> loadPage(true, true));
        retry.setOnClickListener(v -> loadPage(true, true));
        loadMore.setOnClickListener(v -> loadPage(false, false));
        root.findViewById(R.id.ai_agents_back).setOnClickListener(v -> closeMarketplace());
        root.findViewById(R.id.ai_agents_search_button).setOnClickListener(v -> submitSearch());
        clearSearch.setOnClickListener(v -> {
            search.setText("");
            submitSearch();
        });
        search.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_SEARCH) return false;
            submitSearch();
            return true;
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { }
            @Override public void afterTextChanged(Editable editable) {
                if (clearSearch == null) return;
                String query = editable == null ? "" : editable.toString().trim();
                clearSearch.setVisibility(query.isEmpty() ? View.GONE : View.VISIBLE);
                if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
                if (query.isEmpty() || query.length() >= 2) {
                    pendingSearch = () -> {
                        if (isViewAlive()) loadPage(true, false);
                    };
                    searchHandler.postDelayed(pendingSearch, SEARCH_DEBOUNCE_MS);
                }
            }
        });

        setupCategoryChips();

        loadPage(true, false);
        return root;
    }

    private void submitSearch() {
        if (getContext() == null) return;
        if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
        InputMethodManager keyboard = (InputMethodManager) requireContext()
                .getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(search.getWindowToken(), 0);
        search.clearFocus();
        loadPage(true, false);
    }

    private void setupCategoryChips() {
        int initialChipId = View.NO_ID;
        for (int i = 0; i < CATEGORY_LABELS.length; i++) {
            RadioButton option = new RadioButton(requireContext());
            option.setId(View.generateViewId());
            option.setText(CATEGORY_LABELS[i]);
            option.setButtonDrawable(null);
            option.setBackgroundResource(R.drawable.bg_ai_agent_category);
            option.setTextColor(requireContext().getColorStateList(R.color.ai_agent_category_text));
            option.setGravity(android.view.Gravity.CENTER);
            option.setMinHeight(dp(40));
            option.setPadding(dp(16), dp(8), dp(16), dp(8));
            option.setTag(i);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(
                    RadioGroup.LayoutParams.WRAP_CONTENT, RadioGroup.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(dp(8));
            categories.addView(option, params);
            if (i == selectedCategoryIndex) initialChipId = option.getId();
        }
        if (initialChipId != View.NO_ID) categories.check(initialChipId);
        categories.setOnCheckedChangeListener((group, checkedId) -> {
            View selected = group.findViewById(checkedId);
            if (selected == null || !(selected.getTag() instanceof Integer)) return;
            int newIndex = (Integer) selected.getTag();
            if (newIndex == selectedCategoryIndex) return;
            selectedCategoryIndex = newIndex;
            loadPage(true, false);
        });
    }

    private void loadPage(boolean reset, boolean forceNetwork) {
        if (!isAdded() || (!reset && loading)) {
            if (refreshLayout != null) refreshLayout.setRefreshing(false);
            return;
        }
        int requestedPage = reset ? 1 : currentPage + 1;
        int generation = ++pageGeneration;
        loading = true;
        if (activePageCall != null) activePageCall.cancel();
        stopPageLoadTrace();
        pageLoadTrace = AppTelemetry.startTrace("ai_agent_catalogue_load");
        showLoading(reset);
        String query = search == null ? "" : search.getText().toString();
        String tag = selectedCategoryIndex >= 0 && selectedCategoryIndex < CATEGORY_TAGS.length
                ? CATEGORY_TAGS[selectedCategoryIndex] : "";
        activePageCall = repository.loadAgents(requestedPage, PAGE_SIZE, query, tag, forceNetwork,
                new CrooCatalogueRepository.PageCallback() {
                    @Override public void onSuccess(CrooCataloguePage page, boolean fromStaleCache) {
                        if (generation != pageGeneration) return;
                        stopPageLoadTrace();
                        AppTelemetry.logOperationResult("ai_agent_catalogue_load", true);
                        if (!isViewAlive()) return;
                        loading = false;
                        currentPage = requestedPage;
                        total = Math.max(page.getTotal(), page.getAgents().size());
                        if (reset) adapter.replaceWith(page.getAgents());
                        else adapter.appendUnique(page.getAgents());
                        if (reset && adapter.getItemCount() > 0) list.scrollToPosition(0);
                        finishLoading();
                        showListState();
                        if (fromStaleCache && getContext() != null) {
                            Toast.makeText(getContext(), R.string.ai_agents_saved, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override public void onError() {
                        if (generation != pageGeneration) return;
                        stopPageLoadTrace();
                        AppTelemetry.logOperationResult("ai_agent_catalogue_load", false);
                        if (!isViewAlive()) return;
                        loading = false;
                        finishLoading();
                        if (adapter.getItemCount() == 0) showError();
                        else Toast.makeText(requireContext(), R.string.ai_agents_error, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showLoading(boolean reset) {
        retry.setVisibility(View.GONE);
        state.setVisibility(View.GONE);
        loadMore.setEnabled(false);
        if (reset && adapter.getItemCount() == 0 && !refreshLayout.isRefreshing()) {
            progress.setVisibility(View.VISIBLE);
            list.setVisibility(View.GONE);
        }
    }

    private void finishLoading() {
        progress.setVisibility(View.GONE);
        refreshLayout.setRefreshing(false);
        loadMore.setEnabled(true);
    }

    private void stopPageLoadTrace() {
        if (pageLoadTrace == null) return;
        pageLoadTrace.stop();
        pageLoadTrace = null;
    }

    private void showListState() {
        boolean empty = adapter.getItemCount() == 0;
        list.setVisibility(empty ? View.GONE : View.VISIBLE);
        state.setVisibility(empty ? View.VISIBLE : View.GONE);
        stateText.setText(R.string.ai_agents_empty);
        retry.setVisibility(View.GONE);
        loadMore.setVisibility(!empty && adapter.getItemCount() < total ? View.VISIBLE : View.GONE);
        resultSummary.setVisibility(empty ? View.GONE : View.VISIBLE);
        resultSummary.setText(getResources().getQuantityString(
                R.plurals.ai_agents_result_count, total, total));
    }

    private void showError() {
        list.setVisibility(View.GONE);
        loadMore.setVisibility(View.GONE);
        state.setVisibility(View.VISIBLE);
        stateText.setText(R.string.ai_agents_error);
        retry.setVisibility(View.VISIBLE);
        resultSummary.setVisibility(View.GONE);
    }

    private boolean isViewAlive() {
        return isAdded() && getView() != null;
    }

    private void loadAgentDetails(CrooAgent catalogueAgent) {
        if (!isAdded() || activeDialog != null && activeDialog.isShowing()) return;
        if (activeAgentCall != null) activeAgentCall.cancel();
        int generation = ++agentGeneration;
        activeDialog = new AlertDialog.Builder(requireContext())
                .setTitle(catalogueAgent.getName())
                .setMessage(R.string.ai_agents_loading_services)
                .setNegativeButton(R.string.cancel, null)
                .create();
        activeDialog.setOnDismissListener(dialog -> {
            agentGeneration++;
            if (activeAgentCall != null) activeAgentCall.cancel();
            activeDialog = null;
        });
        activeDialog.show();
        activeAgentCall = repository.loadAgent(catalogueAgent.getId(), new CrooCatalogueRepository.AgentCallback() {
            @Override public void onSuccess(CrooAgent agent) {
                if (!isViewAlive() || generation != agentGeneration) return;
                dismissActiveDialog();
                showAgentSummary(agent);
            }

            @Override public void onError() {
                if (!isViewAlive() || generation != agentGeneration) return;
                dismissActiveDialog();
                new AlertDialog.Builder(requireContext())
                        .setTitle(R.string.ai_agents_agent_details)
                        .setMessage(R.string.ai_agents_detail_error)
                        .setPositiveButton(R.string.retry, (dialog, which) -> loadAgentDetails(catalogueAgent))
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        });
    }

    private void showAgentSummary(CrooAgent agent) {
        if (!isAdded()) return;
        String status = getString(agent.isOnline() ? R.string.ai_agents_online : R.string.ai_agents_offline);
        String message = status + "\n" + getString(R.string.ai_agents_from_price_orders,
                agent.getFormattedMinimumPrice(), agent.getCompletedOrders());
        if (!agent.getAverageDelivery().isEmpty()) message += "\n" + agent.getAverageDelivery();
        if (!agent.getDescription().isEmpty()) message += "\n\n" + agent.getDescription();
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext())
                .setTitle(agent.getName())
                .setMessage(message)
                .setNegativeButton(R.string.cancel, null);
        if (agent.getServices().isEmpty()) {
            builder.setPositiveButton(R.string.ai_agents_no_services, null);
        } else {
            builder.setPositiveButton(R.string.ai_agents_services, (dialog, which) -> showServices(agent));
        }
        builder.show();
    }

    private void showServices(CrooAgent agent) {
        if (!isAdded()) return;
        List<CrooService> services = agent.getServices();
        if (services.isEmpty()) return;
        final int[] selectedIndex = {-1};
        ArrayAdapter<CrooService> serviceAdapter = new ArrayAdapter<CrooService>(requireContext(),
                R.layout.item_ai_agent_service, android.R.id.text1, services) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                CheckedTextView row = (CheckedTextView) super.getView(position, convertView, parent);
                CrooService service = getItem(position);
                if (service == null) return row;
                String sla = service.getSlaMinutes() > 0
                        ? getString(R.string.ai_agents_minutes, service.getSlaMinutes())
                        : getString(R.string.ai_agents_not_specified);
                String title = service.getName();
                String summary = getString(R.string.ai_agents_service_summary,
                        service.getFormattedPrice(), sla, service.getOrders7d());
                SpannableString label = new SpannableString(title + "\n" + summary);
                label.setSpan(new StyleSpan(Typeface.BOLD), 0, title.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                label.setSpan(new ForegroundColorSpan(ContextCompat.getColor(requireContext(),
                                R.color.colorSecondaryText)), title.length() + 1, label.length(),
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                row.setText(label);
                GradientDrawable identity = new GradientDrawable();
                identity.setColor(ContextCompat.getColor(requireContext(),
                        SERVICE_ACCENT_COLORS[position % SERVICE_ACCENT_COLORS.length]));
                identity.setCornerRadius(dp(3));
                identity.setSize(dp(6), dp(44));
                row.setCompoundDrawablePadding(dp(12));
                row.setCompoundDrawablesRelativeWithIntrinsicBounds(identity, null, null, null);
                return row;
            }
        };
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.ai_agents_services)
                .setSingleChoiceItems(serviceAdapter, -1, (selectionDialog, which) -> {
                    if (which < 0 || which >= services.size()) return;
                    selectedIndex[0] = which;
                    ((AlertDialog) selectionDialog).getButton(AlertDialog.BUTTON_POSITIVE)
                            .setEnabled(true);
                })
                .setPositiveButton(R.string.ok, null)
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(ignored -> {
            Button proceed = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            proceed.setEnabled(false);
            proceed.setOnClickListener(view -> {
                int index = selectedIndex[0];
                if (index < 0 || index >= services.size()) return;
                dialog.dismiss();
                showCheckoutReview(agent, services.get(index));
            });
        });
        activeDialog = dialog;
        dialog.setOnDismissListener(ignored -> {
            if (activeDialog == dialog) activeDialog = null;
        });
        dialog.show();
    }

    private void showCheckoutReview(CrooAgent agent, CrooService service) {
        if (!isAdded()) return;
        String wallet = PreferenceManager.getWalletAddress(requireContext().getApplicationContext());
        StringBuilder message = new StringBuilder(getString(R.string.ai_agents_checkout_notice,
                service.getName(), service.getFormattedPrice(), shortAddress(wallet)));
        if (!service.getRequirements().isEmpty()) {
            message.append("\n\n").append(getString(R.string.ai_agents_requirements))
                    .append(":\n").append(service.getRequirements());
        }
        if (!service.getDeliverable().isEmpty()) {
            message.append("\n\n").append(getString(R.string.ai_agents_deliverable))
                    .append(":\n").append(service.getDeliverable());
        }
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.ai_agents_checkout_title)
                .setMessage(message.toString())
                .setPositiveButton(R.string.ai_agents_hire, (dialog, which) -> continueToCroo(agent))
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void continueToCroo(CrooAgent agent) {
        if (getContext() == null) return;
        String wallet = PreferenceManager.getWalletAddress(requireContext().getApplicationContext());
        if (wallet == null || wallet.trim().isEmpty()) {
            Toast.makeText(requireContext(), R.string.ai_agents_wallet_required, Toast.LENGTH_LONG).show();
            return;
        }
        String mnemonic = PreferenceManager.getWalletMnemonic(requireContext().getApplicationContext());
        String password = PreferenceManager.getWalletPassword(requireContext().getApplicationContext());
        if (mnemonic == null || mnemonic.trim().isEmpty() || password == null) {
            Toast.makeText(requireContext(), R.string.ai_agents_wallet_signing_unavailable, Toast.LENGTH_LONG).show();
            return;
        }
        Fragment parent = getParentFragment();
        if (parent instanceof WalletFragment) {
            ((WalletFragment) parent).openCrooAgentCheckout(agent.getId());
        }
    }

    private void closeMarketplace() {
        Fragment parent = getParentFragment();
        if (parent instanceof WalletFragment) ((WalletFragment) parent).closeWalletChild();
    }

    private void dismissActiveDialog() {
        if (activeDialog != null) {
            activeDialog.setOnDismissListener(null);
            activeDialog.dismiss();
            activeDialog = null;
        }
    }

    private static String shortAddress(String address) {
        String value = address == null ? "" : address.trim();
        return value.length() > 12
                ? value.substring(0, 6) + "…" + value.substring(value.length() - 4)
                : value;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onDestroyView() {
        pageGeneration++;
        agentGeneration++;
        stopPageLoadTrace();
        if (activePageCall != null) activePageCall.cancel();
        if (activeAgentCall != null) activeAgentCall.cancel();
        if (pendingSearch != null) searchHandler.removeCallbacks(pendingSearch);
        dismissActiveDialog();
        if (list != null) list.setAdapter(null);
        adapter = null;
        refreshLayout = null;
        list = null;
        progress = null;
        state = null;
        stateText = null;
        retry = null;
        loadMore = null;
        search = null;
        clearSearch = null;
        categories = null;
        resultSummary = null;
        super.onDestroyView();
    }
}
