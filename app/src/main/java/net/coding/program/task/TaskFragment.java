package net.coding.program.task;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.view.menu.ActionMenuItemView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import net.coding.program.MainActivity;
import net.coding.program.MyApp;
import net.coding.program.R;
import net.coding.program.common.FilterDialog;
import net.coding.program.common.Global;
import net.coding.program.common.ListModify;
import net.coding.program.common.SaveFragmentPagerAdapter;
import net.coding.program.common.network.LoadingFragment;
import net.coding.program.event.EventFilter;
import net.coding.program.event.EventFilterDetail;
import net.coding.program.message.JSONUtils;
import net.coding.program.model.AccountInfo;
import net.coding.program.model.ProjectObject;
import net.coding.program.model.TaskLabelModel;
import net.coding.program.model.TaskObject;
import net.coding.program.project.detail.TaskListFragment;
import net.coding.program.project.detail.TaskListFragment_;
import net.coding.program.task.add.TaskAddActivity_;
import net.coding.program.third.MyPagerSlidingTabStrip;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.OnActivityResult;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.ViewById;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;


@EFragment(R.layout.fragment_task)
@OptionsMenu(R.menu.fragment_task)
public class TaskFragment extends LoadingFragment implements TaskListParentUpdate {

    final String host = Global.HOST_API + "/projects?pageSize=100&type=all";
    final String urlTaskCount = Global.HOST_API + "/tasks/projects/count";
    final String urlTaskLabels = Global.HOST_API + "/v2/tasks/search_filters";

    @ViewById
    MyPagerSlidingTabStrip tabs;
    @ViewById(R.id.pagerTaskFragment)
    ViewPager pager;
    @ViewById
    View actionDivideLine;

    ArrayList<ProjectObject> mData = new ArrayList<>();
    ArrayList<ProjectObject> mAllData = new ArrayList<>();
    int pageMargin;
    private PageTaskFragment adapter;
    private TextView toolBarTitle;

    //任务筛选
    List<TaskLabelModel> taskLabelModels = new ArrayList<>();
    private final String[] mMeActions = new String[]{"owner", "watcher", "creator"};
    private FilterDialog.FilterModel mFilterModel;
    private int statusIndex = 0;////筛选的index

    @AfterViews
    void initTaskFragment() {
        pageMargin = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, getResources()
                .getDisplayMetrics());

        tabs.setLayoutInflater(mInflater);

        getNetwork(host, host);
        //加载所有的标签
        getNetwork(urlTaskLabels, urlTaskLabels);

        adapter = new PageTaskFragment(getChildFragmentManager());
        pager.setPageMargin(pageMargin);
        pager.setAdapter(adapter);

        tabs.setVisibility(View.INVISIBLE);
        actionDivideLine.setVisibility(View.INVISIBLE);

        toolBarTitle = (TextView) getActivity().findViewById(R.id.toolbarProjectTitle);
        showLoading(true);
    }

    @Override
    public void onRefresh() {
    }

    private void initListData() {
        mAllData.clear();
        mData.clear();
        mData.add(new ProjectObject());

        try {
            JSONObject json = AccountInfo.getGetRequestCacheData(getActivity(), host);
            jsonToAllData(json.optJSONArray("list"));

            JSONArray jsonArray = AccountInfo.getGetRequestCacheListData(getActivity(), urlTaskCount);
            jsonToData(jsonArray);

        } catch (Exception e) {
            Global.errorLog(e);
        }
    }

    @Override
    public void parseJson(int code, JSONObject respanse, String tag, int pos, Object data) throws JSONException {
        if (tag.equals(urlTaskLabels)) {
            if (code == 0) {
                taskLabelModels = JSONUtils.getList("labels", respanse.getString("data"), TaskLabelModel.class);
            }
        } else if (tag.equals(host)) {
            if (code == 0) {
                JSONArray jsonArray = respanse.getJSONObject("data").getJSONArray("list");

                jsonToAllData(jsonArray);

                getNetwork(urlTaskCount, urlTaskCount);
            } else {
                showErrorMsg(code, respanse);
            }
        } else if (tag.equals(urlTaskCount)) {
            showLoading(false);
            if (code == 0) {
                JSONArray jsonArray = respanse.getJSONArray("data");
                jsonToData(jsonArray);

                tabs.setVisibility(View.VISIBLE);
                actionDivideLine.setVisibility(View.VISIBLE);
                tabs.setViewPager(pager);
                adapter.notifyDataSetChanged();

                Activity activity = getActivity();
                if (activity instanceof MainActivity) {
                    ((MainActivity) activity).hideActionBarShadow();
                }

            } else {
                showErrorMsg(code, respanse);
            }
        }
    }

    private void jsonToAllData(JSONArray jsonArray) throws JSONException {
        mAllData.clear();
        for (int i = 0; i < jsonArray.length(); ++i) {
            ProjectObject projectObject = new ProjectObject(jsonArray.getJSONObject(i));
            mAllData.add(projectObject);
        }
    }

    private void jsonToData(JSONArray jsonArray) throws JSONException {
        mData.clear();
        mData.add(new ProjectObject());

        for (int i = 0; i < jsonArray.length(); ++i) {
            TaskCount taskCount = new TaskCount(jsonArray.getJSONObject(i));
            for (int j = 0; j < mAllData.size(); ++j) {
                ProjectObject project = mAllData.get(j);
                if (taskCount.project == project.getId()) {
                    mData.add(project);
                }
            }
        }
    }

    @OnActivityResult(ListModify.RESULT_EDIT_LIST)
    void onResultEditList(int resultCode) {
        if (resultCode == Activity.RESULT_OK) {
            taskListParentUpdate();
        }
    }

    @Override
    public void taskListParentUpdate() {
        List<WeakReference<Fragment>> array = adapter.getFragments();
        for (WeakReference<Fragment> item : array) {
            Fragment fragment = item.get();
            if (fragment instanceof TaskListUpdate) {
                ((TaskListUpdate) fragment).taskListUpdate(true);
            }
        }
    }

    @OptionsItem
    protected final void action_add() {
        ProjectObject projectObject = mData.get(pager.getCurrentItem());
        TaskAddActivity_.intent(this)
                .mUserOwner(MyApp.sUserObject)
                .mProjectObject(projectObject)
                .startForResult(ListModify.RESULT_EDIT_LIST);
    }

    public static class TaskCount {
        public int project;
        public int processing;
        public int done;

        public TaskCount(JSONObject json) throws JSONException {
            project = json.optInt("project");
            processing = json.optInt("processing");
            done = json.optInt("done");
        }
    }

    private class PageTaskFragment extends SaveFragmentPagerAdapter implements MyPagerSlidingTabStrip.IconTabProvider {

        public PageTaskFragment(android.support.v4.app.FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mData.get(position).name;
        }

        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            TaskListFragment fragment = (TaskListFragment) super.instantiateItem(container, position);
            fragment.setParent(TaskFragment.this);

            Log.d("", "init p " + position);

            return fragment;
        }

        @Override
        public int getItemPosition(Object object) {
            return PagerAdapter.POSITION_NONE;
        }

        @Override
        public String getPageIconUrl(int position) {
            return mData.get(position).icon;
        }

        @Override
        public Fragment getItem(int position) {
            TaskListFragment_ fragment = new TaskListFragment_();
            Bundle bundle = new Bundle();
            bundle.putSerializable("mMembers", new TaskObject.Members(AccountInfo.loadAccount(getActivity())));
            bundle.putSerializable("mProjectObject", mData.get(position));
            bundle.putBoolean("mShowAdd", false);

            bundle.putString("mMeAction", mMeActions[statusIndex]);
            if (mFilterModel != null) {
                bundle.putString("mStatus", mFilterModel.status + "");
                bundle.putString("mLabel", mFilterModel.label);
                bundle.putString("mKeyword", mFilterModel.keyword);
                changeFilterIcon(mFilterModel.isFilter());
            }else{
                bundle.putString("mStatus", "");
                bundle.putString("mLabel",  "");
                bundle.putString("mKeyword",  "");
            }
            fragment.setArguments(bundle);

            saveFragment(fragment);

            return fragment;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        EventBus.getDefault().unregister(this);
    }

    // 用于处理推送
    public void onEventMainThread(Object object) {
        if (!(object instanceof EventFilter)) {
            return;
        }
        EventFilter eventFilter = (EventFilter) object;
        //确定是我的任务筛选
        if (eventFilter.index == 1) {
            iniTaskStatusLayout();
            iniTaskStatus();
        }
    }

    private void iniTaskStatusLayout() {
        if (getActivity() == null) return;

        View viewById = getActivity().findViewById(R.id.ll_task_filter);
        viewById.setVisibility(viewById.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }


    private void iniTaskStatus() {
        if (getActivity() == null) return;

        int[] filterItem = {R.id.tv_status1, R.id.tv_status2, R.id.tv_status3};
        int font2 = getResources().getColor(R.color.font_2);
        int green = getResources().getColor(R.color.green);
        for (int i = 0; i < filterItem.length; i++) {
            TextView status = (TextView) getActivity().findViewById(filterItem[i]);
            int finalI = i;
            status.setOnClickListener(v -> {
                this.statusIndex = finalI;
                toolBarTitle.setText(status.getText());
                iniTaskStatus();
                iniTaskStatusLayout();
                sureFilter();
            });
            status.setTextColor(i != this.statusIndex ? font2 : green);
            status.setCompoundDrawablesWithIntrinsicBounds(0, 0, i != this.statusIndex ? 0 : R.drawable.ic_task_status_list_check, 0);
        }
    }

    private void sureFilter() {
        EventBus.getDefault().post(new EventFilterDetail(mMeActions[statusIndex], mFilterModel));
    }

    @OptionsItem
    protected final void action_filter() {

        if (mFilterModel == null) {
            mFilterModel = new FilterDialog.FilterModel(taskLabelModels);
        } else {
            mFilterModel.labelModels = taskLabelModels;
        }

        FilterDialog.getInstance().show(getContext(), mFilterModel, new FilterDialog.SearchListener() {
            @Override
            public void callback(FilterDialog.FilterModel filterModel) {
                mFilterModel = filterModel;
                sureFilter();
                changeFilterIcon(mFilterModel.isFilter());
            }
        });
    }

    private void changeFilterIcon(boolean isFilter) {
        if (getActivity() == null) return;

        Toolbar mToolbar = (Toolbar) getActivity().findViewById(R.id.toolbar);
        if (mToolbar != null) {
            ActionMenuItemView viewById = (ActionMenuItemView) mToolbar.findViewById(R.id.action_filter);
            if (viewById != null) {
                viewById.setIcon(getResources().getDrawable(isFilter ? R.drawable.ic_menu_filter_selected : R.drawable.ic_menu_filter));
            }
        }
    }
}
