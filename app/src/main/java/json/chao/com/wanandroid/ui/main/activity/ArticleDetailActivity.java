package json.chao.com.wanandroid.ui.main.activity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;

import com.scwang.smartrefresh.layout.SmartRefreshLayout;

import java.lang.reflect.Method;

import javax.inject.Inject;

import butterknife.BindView;
import ezy.ui.layout.LoadingLayout;
import json.chao.com.wanandroid.R;
import json.chao.com.wanandroid.app.Constants;
import json.chao.com.wanandroid.base.activity.BaseActivity;
import json.chao.com.wanandroid.component.RxBus;
import json.chao.com.wanandroid.contract.main.ArticleDetailContract;
import json.chao.com.wanandroid.core.DataManager;
import json.chao.com.wanandroid.core.bean.main.collect.FeedArticleListResponse;
import json.chao.com.wanandroid.core.event.CancelCollectSuccessEvent;
import json.chao.com.wanandroid.core.event.CollectSuccessEvent;
import json.chao.com.wanandroid.presenter.main.ArticleDetailPresenter;
import json.chao.com.wanandroid.utils.CommonUtils;
import json.chao.com.wanandroid.utils.StatusBarUtil;

/**
 * @author quchao
 * @date 2018/2/13
 */

public class ArticleDetailActivity extends BaseActivity<ArticleDetailPresenter> implements ArticleDetailContract.View {

    @BindView(R.id.article_detail_group)
    LinearLayout mWebViewGroup;
    @BindView(R.id.article_detail_toolbar)
    Toolbar mToolbar;
    @BindView(R.id.article_detail_loading_layout)
    LoadingLayout mLoadingLayout;
    @BindView(R.id.article_detail_refresh_Layout)
    SmartRefreshLayout mRefreshLayout;
    @BindView(R.id.article_detail_web_view)
    WebView mWebView;

    private Bundle bundle;
    private MenuItem mCollectItem;
    private int articleId;
    private String articleLink;
    private String title;

    @Inject
    DataManager mDataManager;
    private boolean isCollect;
    private boolean isCommonSite;
    private boolean isCollectPage;

    @Override
    public void onResume() {
        super.onResume();
        mWebView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mWebView.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mWebView != null) {
            mWebView.setWebChromeClient(null);
            mWebView.setWebViewClient(null);
            mWebView.clearHistory();
            mWebView.clearCache(true);
            mWebView.removeAllViews();
            mWebView.destroy();
            mRefreshLayout.removeView(mWebView);
            mWebView = null;
        }
        super.onDestroy();
    }

    @Override
    protected void initInject() {
        getActivityComponent().inject(this);
    }

    @Override
    protected int getLayoutId() {
        return R.layout.activity_article_detail;
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void initEventAndData() {
        initToolBar();

        mWebView.loadUrl(articleLink);
        WebSettings settings = mWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                view.loadUrl(articleLink);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                mLoadingLayout.showContent();
            }
        });
    }

    @Override
    public void onBackPressedSupport() {
        if (mWebView.canGoBack()) {
            mWebView.goBack();
        } else {
            super.onBackPressedSupport();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        bundle = getIntent().getExtras();
        assert bundle != null;
        isCommonSite = (boolean) bundle.get(Constants.IS_COMMON_SITE);
        if (!isCommonSite) {
            getMenuInflater().inflate(R.menu.menu_acticle, menu);
            mCollectItem = menu.findItem(R.id.item_collect);
            if (isCollect) {
                mCollectItem.setTitle(getString(R.string.cancel_collect));
            } else {
                mCollectItem.setTitle(getString(R.string.collect));
            }
        } else {
            getMenuInflater().inflate(R.menu.menu_article_common, menu);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.item_share:
                shareEvent();
                break;
            case R.id.item_collect:
                collectEvent();
                break;
            case R.id.item_system_browser:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(articleLink)));
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * 让菜单同时显示图标和文字
     *
     * @param featureId Feature id
     * @param menu Menu
     * @return menu if opened
     */
    @Override
    public boolean onMenuOpened(int featureId, Menu menu) {
        if (menu != null) {
            if (Constants.MENU_BUILDER.equalsIgnoreCase(menu.getClass().getSimpleName())) {
                try {
                    @SuppressLint("PrivateApi")
                    Method method = menu.getClass().getDeclaredMethod("setOptionalIconsVisible", Boolean.TYPE);
                    method.setAccessible(true);
                    method.invoke(menu, true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return super.onMenuOpened(featureId, menu);
    }

    private void shareEvent() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_type_url, getString(R.string.app_name), title, articleLink));
        intent.setType("text/plain");
        startActivity(intent);
    }

    private void collectEvent() {
        if (!mDataManager.getLoginStatus()) {
            CommonUtils.showMessage(this, getString(R.string.login_tint));
            startActivity(new Intent(this, LoginActivity.class));
        } else {
            if (mCollectItem.getTitle().equals(getString(R.string.collect))) {
                mPresenter.addCollectArticle(articleId);
            } else {
                if (isCollectPage) {
                    mPresenter.cancelCollectPageArticle(articleId);
                } else {
                    mPresenter.cancelCollectArticle(articleId);
                }
            }
        }
    }

    private void initToolBar() {
        bundle = getIntent().getExtras();
        assert bundle != null;
        title = (String) bundle.get(Constants.ARTICLE_TITLE);
        setToolBar(mToolbar, Html.fromHtml(title));
        StatusBarUtil.immersive(this);
        StatusBarUtil.setPaddingSmart(this, mToolbar);
        mToolbar.setNavigationOnClickListener(v -> {
            if (isCollect) {
                RxBus.getDefault().post(new CollectSuccessEvent());
            } else {
                RxBus.getDefault().post(new CancelCollectSuccessEvent());
            }
            onBackPressedSupport();
        });

        articleLink = (String) bundle.get(Constants.ARTICLE_LINK);
        articleId = ((int) bundle.get(Constants.ARTICLE_ID));
        isCommonSite = ((boolean) bundle.get(Constants.IS_COMMON_SITE));
        isCollect = ((boolean) bundle.get(Constants.IS_COLLECT));
        isCollectPage = ((boolean) bundle.get(Constants.IS_COLLECT_PAGE));
    }

    @Override
    public void showCollectArticleData(FeedArticleListResponse feedArticleListResponse) {
        isCollect = true;
        mCollectItem.setTitle(R.string.cancel_collect);
        CommonUtils.showMessage(this, getString(R.string.collect_success));
    }

    @Override
    public void showCancelCollectArticleData(FeedArticleListResponse feedArticleListResponse) {
        isCollect = false;
        if (!isCollectPage) {
            mCollectItem.setTitle(R.string.collect);
        }
        CommonUtils.showMessage(this, getString(R.string.cancel_collect_success));
    }

}
