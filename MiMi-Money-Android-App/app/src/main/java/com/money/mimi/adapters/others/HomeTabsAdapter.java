package com.money.mimi.adapters.others;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import android.util.SparseArray;
import android.view.ViewGroup;

import com.money.mimi.fragments.home.CallsFragment;
import com.money.mimi.fragments.home.ConversationsFragment;
import com.money.mimi.fragments.home.MoneyFragment;
import com.money.mimi.fragments.home.SpaceFragment;
import com.money.mimi.fragments.home.WalletFragment;

/**
 * Created by Abderrahim El imame on 27/02/2016.
 * Email : abderrahim.elimame@gmail.com
 */
public class HomeTabsAdapter extends FragmentStatePagerAdapter {

    private final SparseArray<Fragment> registeredFragments = new SparseArray<>();


    public HomeTabsAdapter(FragmentManager fm) {
        super(fm);
    }


    @Override
    public Fragment getItem(int position) {
        switch (position) {
            case 0:
                return new ConversationsFragment();
            case 1:
                return new CallsFragment();
            case 2:
                return new WalletFragment();
            case 3:
                return new MoneyFragment();
            case 4:
                return new SpaceFragment();
        }
        return null;
    }

    @Override
    public int getCount() {
        return 5;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        Object item = super.instantiateItem(container, position);
        if (item instanceof Fragment) {
            registeredFragments.put(position, (Fragment) item);
        }
        return item;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        registeredFragments.remove(position);
        super.destroyItem(container, position, object);
    }

    @Nullable
    public Fragment getRegisteredFragment(int position) {
        return registeredFragments.get(position);
    }

}