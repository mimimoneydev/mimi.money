package com.money.mimi.adapters.recyclerView.agents;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.money.mimi.R;
import com.money.mimi.agents.CrooAgent;

import java.util.ArrayList;
import java.util.List;

public final class AiAgentAdapter extends RecyclerView.Adapter<AiAgentAdapter.AgentHolder> {
    public interface Listener { void onAgentSelected(CrooAgent agent); }

    private final List<CrooAgent> agents = new ArrayList<>();
    private final Listener listener;

    public AiAgentAdapter(Listener listener) {
        this.listener = listener;
        setHasStableIds(true);
    }

    public void replaceWith(List<CrooAgent> newAgents) {
        int previousSize = agents.size();
        agents.clear();
        if (previousSize > 0) notifyItemRangeRemoved(0, previousSize);
        if (newAgents != null) agents.addAll(newAgents);
        if (!agents.isEmpty()) notifyItemRangeInserted(0, agents.size());
    }

    public void appendUnique(List<CrooAgent> newAgents) {
        if (newAgents == null || newAgents.isEmpty()) return;
        int start = agents.size();
        for (CrooAgent candidate : newAgents) {
            boolean exists = false;
            for (CrooAgent current : agents) {
                if (current.getId().equals(candidate.getId())) {
                    exists = true;
                    break;
                }
            }
            if (!exists) agents.add(candidate);
        }
        int count = agents.size() - start;
        if (count > 0) notifyItemRangeInserted(start, count);
    }

    @Override public long getItemId(int position) { return agents.get(position).getId().hashCode(); }
    @Override public int getItemCount() { return agents.size(); }

    @NonNull
    @Override
    public AgentHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new AgentHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ai_agent, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull AgentHolder holder, int position) {
        CrooAgent agent = agents.get(position);
        Context context = holder.itemView.getContext();
        holder.name.setText(agent.getName());
        holder.description.setText(agent.getDescription());
        holder.description.setVisibility(agent.getDescription().isEmpty() ? View.GONE : View.VISIBLE);
        holder.status.setText(agent.isOnline() ? R.string.ai_agents_online : R.string.ai_agents_offline);
        holder.status.setTextColor(ContextCompat.getColor(context,
                agent.isOnline() ? R.color.colorGreenVeryDark : R.color.colorSecondaryText));
        holder.tags.setText(formatTags(context, agent.getTags()));
        holder.tags.setVisibility(agent.getTags().isEmpty() ? View.GONE : View.VISIBLE);
        holder.price.setText(context.getString(R.string.ai_agents_from_price_orders,
                agent.getFormattedMinimumPrice(), agent.getCompletedOrders()));
        holder.itemView.setOnClickListener(v -> listener.onAgentSelected(agent));

        Glide.clear(holder.avatar);
        holder.avatar.setImageResource(R.drawable.ic_account_24dp);
        if (!agent.getImageUrl().isEmpty()) {
            Glide.with(context).load(agent.getImageUrl())
                    .placeholder(R.drawable.ic_account_24dp)
                    .error(R.drawable.ic_account_24dp)
                    .into(holder.avatar);
        }
    }

    @Override
    public void onViewRecycled(@NonNull AgentHolder holder) {
        Glide.clear(holder.avatar);
        holder.itemView.setOnClickListener(null);
        super.onViewRecycled(holder);
    }

    private static String formatTags(Context context, List<String> tags) {
        StringBuilder result = new StringBuilder();
        for (String tag : tags) {
            if (result.length() > 0) result.append(" · ");
            switch (tag) {
                case "defi-trading": result.append(context.getString(R.string.ai_agents_category_defi)); break;
                case "data-analytics": result.append(context.getString(R.string.ai_agents_category_data)); break;
                case "research-report": result.append(context.getString(R.string.ai_agents_category_research)); break;
                case "development-code": result.append(context.getString(R.string.ai_agents_category_development)); break;
                case "automation-workflow": result.append(context.getString(R.string.ai_agents_category_automation)); break;
                case "content-creative": result.append(context.getString(R.string.ai_agents_category_content)); break;
                case "social-community": result.append(context.getString(R.string.ai_agents_category_social)); break;
                default: result.append(tag.replace('-', ' ')); break;
            }
        }
        return result.toString();
    }

    static final class AgentHolder extends RecyclerView.ViewHolder {
        final ImageView avatar;
        final TextView name;
        final TextView status;
        final TextView description;
        final TextView tags;
        final TextView price;

        AgentHolder(View itemView) {
            super(itemView);
            avatar = itemView.findViewById(R.id.ai_agent_avatar);
            name = itemView.findViewById(R.id.ai_agent_name);
            status = itemView.findViewById(R.id.ai_agent_status);
            description = itemView.findViewById(R.id.ai_agent_description);
            tags = itemView.findViewById(R.id.ai_agent_tags);
            price = itemView.findViewById(R.id.ai_agent_price);
        }
    }
}
