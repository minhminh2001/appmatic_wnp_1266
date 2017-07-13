package com.whitelabel.app.ui.home.presenter;

import com.whitelabel.app.data.DataManager;
import com.whitelabel.app.model.ApiFaildException;
import com.whitelabel.app.model.CategoryDetailModel;
import com.whitelabel.app.model.ResponseModel;
import com.whitelabel.app.ui.RxPresenter;
import com.whitelabel.app.ui.home.HomeCategoryDetailContract;
import com.whitelabel.app.utils.ErrorHandlerAction;
import com.whitelabel.app.utils.RxUtil;

import rx.Subscription;
import rx.functions.Action1;

/**
 * Created by ray on 2017/4/13.
 */

public class HomeCategoryDetailPresenterImpl extends RxPresenter<HomeCategoryDetailContract.View> implements HomeCategoryDetailContract.Presenter{

    public void getCategoryDetail(String categoryId,String sessionKey){
       Subscription subscription= DataManager.getInstance().getProductApi().getCategoryDetail(categoryId,sessionKey)
                .compose(RxUtil.<ResponseModel<CategoryDetailModel>>rxSchedulerHelper())
                .compose(RxUtil.<CategoryDetailModel>handleResult())
                .subscribe(new Action1<CategoryDetailModel>() {
                    @Override
                    public void call(CategoryDetailModel categoryDetailModel) {
                        mView.closeProgressDialog();
                        mView.closeRefreshLaout();
                         mView.loadData(categoryDetailModel);
                    }
                }, new ErrorHandlerAction() {
                    @Override
                    protected void requestError(ApiFaildException ex) {
                        mView.closeProgressDialog();
                        mView.closeRefreshLaout();
                        mView.showErrorMsg(ex.getErrorMsg());
                    }
                });
        addSubscrebe(subscription);
    }

}