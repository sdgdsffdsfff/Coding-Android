package net.coding.program.project;

import android.app.Activity;
import android.content.Intent;
import android.support.v4.app.Fragment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.readystatesoftware.viewbadger.BadgeView;

import net.coding.program.R;
import net.coding.program.common.Global;
import net.coding.program.common.ImageLoadTool;
import net.coding.program.common.model.ProjectObject;
import net.coding.program.common.network.RefreshBaseFragment;
import net.coding.program.param.ProjectJumpParam;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.ViewById;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * Created by Vernon on 15/11/15.
 */
@EFragment(R.layout.project_other_fragment)
public class ProjectOtherFragment extends RefreshBaseFragment {

    final String hostWatched = Global.HOST_API + "/projects?page=1&pageSize=200&type=watched";
    final String hostStared = Global.HOST_API + "/projects?page=1&pageSize=200&type=stared";

    final String[] host = {hostWatched, hostStared};

    boolean requestOk = true;
    boolean needRefresh = true;
    @FragmentArg
    ArrayList<ProjectObject> mData = new ArrayList<>();
    @FragmentArg
    ProjectFragment.Type type = ProjectFragment.Type.Main;
    @ViewById
    StickyListHeadersListView listView;
    @ViewById
    View blankLayout;
    @ViewById
    RelativeLayout project_create_layout;

    boolean mRequestOk;
    private int postion = 0;
    private String title;

    private MyAdapter adapter;

    public void setTitleAndPostion(String title, int postion) {
        this.title = title;
        this.postion = postion;
    }

    @AfterViews
    protected void initProjectOtherFragment() {
        hideDialogLoading();
        setHasOptionsMenu(true);
        initRefreshLayout();
        setRefreshing(true);

        listView.setAreHeadersSticky(false);
        View listViewFooter = getActivity().getLayoutInflater().inflate(R.layout.divide_bottom_15, listView.getWrappedList(), false);
        listView.addFooterView(listViewFooter, null, false);
    }

    private void listView(ProjectObject item) {
        // 在搜索界面不是嵌套的，getParentFragment会返回null
        Fragment fragment = getParentFragment();
        if (fragment == null) {
            fragment = this;
        }

        if (type == ProjectFragment.Type.Main) {
            item.getOwner().global_key = item.project_path.substring(0, item.project_path.indexOf("/p/")).replace("/u/", "");
            ProjectJumpParam param = new ProjectJumpParam(item.getOwner().global_key,
                    item.name);
            ProjectHomeActivity_.intent(fragment).mJumpParam(param).start();
        } else {
            Intent intent = new Intent();
            intent.putExtra("data", item);
            getActivity().setResult(Activity.RESULT_OK, intent);
            getActivity().finish();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (needRefresh) {
            needRefresh = false;
            onRefresh();
        }
    }

    @Override
    protected boolean useEventBus() {
        return true;
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventProjectModify(EventProjectModify event) {
        onRefresh();
    }

    @Override
    public void parseJson(int code, JSONObject respanse, String tag, int pos, Object data) throws JSONException {
        if (tag.equals(host[postion])) {
            setRefreshing(false);
            if (code == 0) {
                requestOk = true;
                mData.clear();
                JSONArray array = respanse.getJSONObject("data").getJSONArray("list");
                int pinCount = 0;
                for (int i = 0; i < array.length(); ++i) {
                    JSONObject item = array.getJSONObject(i);
                    ProjectObject oneData = new ProjectObject(item);
                    if (oneData.isPin()) {
                        mData.add(pinCount++, oneData);
                    } else {
                        mData.add(oneData);
                    }
                }

                if (adapter == null) {
                    adapter = new MyAdapter();
                    listView.setAdapter(adapter);
                    listView.setOnItemClickListener((parent, view, position, id) ->
                            listView((ProjectObject) adapter.getItem((int) id)));
                } else {
                    adapter.notifyDataSetChanged();
                }
                if (!(mData.size() > 0)) {
                    project_create_layout.setVisibility(View.VISIBLE);
                } else {
                    project_create_layout.setVisibility(View.GONE);
                }
            } else {
                requestOk = false;
                showErrorMsg(code, respanse);
//                adapter.notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onRefresh() {
        getNetwork(host[postion], host[postion]);
    }

    public String getTitle() {
        return title;
    }

    private static class ViewHolder {
        TextView name2;
        TextView tv_star_count;
        TextView tv_follow_count;
        TextView tv_fork_count;
        LinearLayout ll_bottom_menu;
        ImageView image;
        TextView desc;
        TextView content;
        BadgeView badge;
        View privateIcon;
    }

    class MyAdapter extends BaseAdapter implements StickyListHeadersAdapter {
        @Override
        public int getCount() {
            return mData.size();
        }

        @Override
        public Object getItem(int position) {
            return mData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            final ViewHolder holder;
            if (convertView == null) {
                view = mInflater.inflate(R.layout.project_all_list_item, parent, false);
                holder = new ViewHolder();
                holder.name2 = (TextView) view.findViewById(R.id.name2);
                holder.image = (ImageView) view.findViewById(R.id.icon);
                holder.content = (TextView) view.findViewById(R.id.comment);
                holder.badge = (BadgeView) view.findViewById(R.id.badge);
                holder.privateIcon = view.findViewById(R.id.privateIcon);
                holder.tv_follow_count = (TextView) view.findViewById(R.id.tv_follow_count);
                holder.tv_fork_count = (TextView) view.findViewById(R.id.tv_fork_count);
                holder.tv_star_count = (TextView) view.findViewById(R.id.tv_start_count);
                holder.ll_bottom_menu = (LinearLayout) view.findViewById(R.id.ll_bottom_menu);
                holder.desc = (TextView) view.findViewById(R.id.txtDesc);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            ProjectObject item = (ProjectObject) getItem(position);

            holder.name2.setText(item.name);
            holder.name2.setVisibility(View.VISIBLE);
            holder.privateIcon.setVisibility(item.isPublic() ? View.INVISIBLE : View.VISIBLE);
            String ownerName = item.isPublic() ? item.owner_user_name : ("      " + item.owner_user_name);
            holder.content.setText(ownerName);
            holder.desc.setText(item.getDescription());
            holder.tv_follow_count.setText(item.getWatchCountString());
            holder.tv_star_count.setText(item.getStarString());
            holder.tv_fork_count.setText(item.getForkCountString());

            if (type == ProjectFragment.Type.Pick) {
                holder.badge.setVisibility(View.INVISIBLE);
            } else {
                int count = item.unReadActivitiesCount;
                BadgeView badge = holder.badge;
                Global.setBadgeView(badge, count);
            }
            if (postion == 0 || postion == 2) {
                holder.content.setVisibility(View.GONE);
                holder.ll_bottom_menu.setVisibility(View.VISIBLE);
            } else {
                holder.content.setVisibility(View.VISIBLE);
                holder.ll_bottom_menu.setVisibility(View.GONE);
            }
            iconfromNetwork(holder.image, item.icon, ImageLoadTool.optionsRounded2);

            return view;
        }

        @Override
        public View getHeaderView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new View(parent.getContext());
            }
            return convertView;
        }

        @Override
        public long getHeaderId(int i) {
            return 0;
        }
    }
}
