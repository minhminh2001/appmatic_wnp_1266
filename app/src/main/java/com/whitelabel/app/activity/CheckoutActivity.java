package com.whitelabel.app.activity;

import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.molpay.molpayxdk.MOLPayActivity;
import com.whitelabel.app.GlobalData;
import com.whitelabel.app.R;
import com.whitelabel.app.adapter.DialogProductAdapter;
import com.whitelabel.app.application.GemfiveApplication;
import com.whitelabel.app.dao.CheckoutDao;
import com.whitelabel.app.dao.MyAccountDao;
import com.whitelabel.app.dao.ProductDao;
import com.whitelabel.app.dao.ShoppingCarDao;
import com.whitelabel.app.fragment.CheckoutPaymentFragment;
import com.whitelabel.app.fragment.CheckoutReviewFragment;
import com.whitelabel.app.fragment.CheckoutShippingAddaddressFragment;
import com.whitelabel.app.fragment.CheckoutShippingDefaultaddressFragment;
import com.whitelabel.app.fragment.CheckoutShippingSelectaddressFragment;
import com.whitelabel.app.model.AddressParameter;
import com.whitelabel.app.model.CheckoutDefaultShippingAddress;
import com.whitelabel.app.model.CheckoutPaymentSaveReturnEntity;
import com.whitelabel.app.model.DialogProductBean;
import com.whitelabel.app.model.GOUserEntity;
import com.whitelabel.app.model.MolpayResponseBean;
import com.whitelabel.app.model.SVRAddAddress;
import com.whitelabel.app.model.SVRAppserviceSaveBillingEntity;
import com.whitelabel.app.model.SVRAppserviceSaveOrderReturnEntity;
import com.whitelabel.app.model.ShoppingDiscountBean;
import com.whitelabel.app.network.ImageLoader;
import com.whitelabel.app.utils.GaTrackHelper;
import com.whitelabel.app.utils.JDataUtils;
import com.whitelabel.app.utils.JJsonUtils;
import com.whitelabel.app.utils.JLocalMethod;
import com.whitelabel.app.utils.JLogUtils;
import com.whitelabel.app.utils.JToolUtils;
import com.whitelabel.app.utils.JViewUtils;
import com.whitelabel.app.utils.RequestErrorHelper;
import com.whitelabel.app.widget.CheckoutPaymentDialog;
import com.whitelabel.app.widget.MaterialDialog;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CheckoutActivity extends com.whitelabel.app.BaseActivity implements View.OnClickListener {

    public Long mGATrackCheckoutTimeStart = 0L;
    public boolean mGATrackCheckoutTimeEnable = false;
    public boolean mGATrackAddressToPaymentTimeEnable = false;
    public Long mGATrackAddressToPaymentTimeStart = 0L;
    public boolean mGATrackPaymentToReviewTimeEnable = false;
    public Long mGATrackPaymentToReviewTimeStart = 0L;
    public Long mGATrackPlaceOrderToResultTimeStart = 0L;
    private String errorProductTitle;
    private TextView tvMenuPayment;
    private TextView tvMenuReview;
    private TextView tvMenuShipping;
    private TextView tvSliderFirst;
    private TextView tvSliderSecond;
    private TextView tvSliderThird;
    private ImageView ivArrow;
    public TextView btnContinue;
    public ScrollView scrollViewBody;
    public LinearLayout llBody, ll_btn;
    public ProgressBar progressBarLoading;
    private FragmentTransaction fragmentTransaction;
    private Fragment checkoutShippingSelectaddressFragment;
    private Fragment checkoutPaymentFragment;
    private Fragment checkoutReviewFragment;
    public String TAG = CheckoutActivity.class.getSimpleName();
    private String ONLINEBANK;
    public ArrayList<Fragment> list_fragment;
    private boolean updateService = false;
    private Button updateVersion;
    private WebView mwebView;
    private boolean existVending = false;
    private String order_id;
    String amount = "";
    String shippingFee = "";
    /**
     * record that if fragment has entered into payment module.
     * initialized value is true because it must be in shipping model in the beginning.
     */
    private boolean neverEnterIntoNext = true;
    /**
     * 1. to record fragment count of Shipping Module for Go Back Button.
     * 2. making this variable public in order to be seen in Fragment
     * 3. it's size need to less than 3 : defaultAddressFragment,selectAddressFragment,addAddressFragment
     * and more than 1 : defaultAddressFragment or addAddressFragment
     */
    public ArrayList<Fragment> list_fragment_shipping;
    /**
     * server response data:
     * 0: having no address,now it should use AddaddressFragment or edit an address.
     * 1: having a default adrress,now it should use SelectaddressFragment without top imageButton and show just one address.
     */
    public String addressConditionInShipping;
    /**
     * record the position of current module:(shipping,payment,review)
     * from left to right -- 1,2,3
     */
    private int currentModule = 1;
    /**
     * slider's animation
     */
    private AnimationSet animationSet;
    /**
     * is "go back" operation or not
     */
    private DataHandler mHandler = new DataHandler(this);
    private boolean isGoBack = false;
    //    public static String SESSION_KEY = "2e4d2348e09dfcaf189453d99b3a4589";
    private final String SESSION_EXPIRED = "session expired,login again please";
    private final int REQUESTCODE_LOGIN = 1000;
    private CheckoutPaymentDialog mCheckoutPaymentDialog = null;//place order dialog
    private Dialog mDialog;//common loading dialog
    private static final int REQUEST_CODE_MOLPAY = 1;
    private ShoppingDiscountBean mDiscountBean;
    private String grandTotal;//add grand total to judge if it is 0 and then checkout to pay or not.--russell
    public int fromType;
    private int skipPayment;
    private MyAccountDao mAccountDao;
    private CheckoutDao mCheckoutDao;
    private ShoppingCarDao mShoppingCarDao;
//    public String paymentMethodCode;
//    public String molpayType;
//    private int paymethodType;//1 online 2credit 3.offline payment
//    private String bank;
    /**
     * send request to place order
     */
    private CheckoutPaymentSaveReturnEntity paymentSaveReturnEntity;
    private ImageLoader mImageLoader;

    static {
        System.loadLibrary("gemfivelocal");
    }

    private View.OnClickListener updateListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            List<PackageInfo> packages = getPackageManager().getInstalledPackages(0);
            for (int i = 0; i < packages.size(); i++) {
                PackageInfo packageInfo = packages.get(i);
                String packgeName = "";
                packgeName = packageInfo.packageName;
                JLogUtils.i("Allen", "packge=" + packgeName);
                if (packgeName.contains("vending")) {
                    //跳转进市场搜索的代码
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setData(Uri.parse(GlobalData.jumpMarketUrl));
                    startActivity(intent);
                    existVending = true;
                }
            }
            if (!existVending) {
                Uri uri = Uri.parse("http://play.google.com/store/apps/details?id=com.whitelabel.app");
                Intent it = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(it);
                existVending = false;
            }
        }
    };

    public void IsOldVersion() {
        mCheckoutPaymentDialog = new CheckoutPaymentDialog(CheckoutActivity.this, R.style.loading_dialog, getResources().getString(R.string.dialog_checkout_text)).showDialog();
        btnContinue.setEnabled(false);
        btnContinue.setBackgroundResource(R.drawable.big_button_style_b8);
        new ProductDao(TAG, mHandler).checkVersion("2");
    }


    public void closeCheckoutPaymentDialog() {
        if (mCheckoutPaymentDialog != null && mCheckoutPaymentDialog.isShowing()) {
            mCheckoutPaymentDialog.dismiss();
        }

    }

    /**
     * e
     * to record fragment(module) count
     * less than 3: shippingFragment,paymentFragment,reviewFragment.
     * more than 1: at least shippingFragment.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);
        initIntent();
        reInit();
        initView();
        initData();
        initToolBar();
    }

    private void initToolBar() {
        setTitle(getResources().getString(R.string.CHECKOUT));
        setLeftMenuIcon(JToolUtils.getDrawable(R.drawable.action_back));
        setLeftMenuClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goBackOption();
            }
        });
    }

    private String productIds;

    private void initIntent() {
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mDiscountBean = (ShoppingDiscountBean) bundle.getSerializable("discountBean");
            grandTotal = bundle.getString("grandTotal");
            fromType = bundle.getInt("fromType");
            productIds = bundle.getString("productIds");
            mGATrackCheckoutTimeStart = bundle.getLong("mGATrackTimeStart", 0L);
            mGATrackCheckoutTimeEnable = true;
        }
    }

    public void expireStartLoginActivity() {
        Bundle bundle = new Bundle();
        bundle.putBoolean("expire", true);
        BottomtoTopActivity(bundle, LoginRegisterActivity.class, false);
    }

    private void initData() {
        mAccountDao = new MyAccountDao(TAG, mHandler);
        mCheckoutDao = new CheckoutDao(TAG, mHandler);
        mShoppingCarDao = new ShoppingCarDao(TAG, mHandler);
        ONLINEBANK = getResources().getString(R.string.payment_method_ONLINE_BANKING);
        errorProductTitle = getResources().getString(R.string.checkout_product_error_title);
        list_fragment = new ArrayList<Fragment>();
        list_fragment_shipping = new ArrayList<Fragment>();
        if (GemfiveApplication.getAppConfiguration().isSignIn(CheckoutActivity.this)) {
            setButtonEnable(true);
            openSelectFragment();
        } else {
            expireStartLoginActivity();
        }
    }

    public void reInit() {
        checkoutShippingSelectaddressFragment = getFragmentManager().findFragmentByTag("selectAddressFragment");
        checkoutPaymentFragment = getFragmentManager().findFragmentByTag("paymentFragment");
        checkoutReviewFragment = getFragmentManager().findFragmentByTag("reviewFragment");
    }

    public void hiddenAll() {
        if (checkoutShippingSelectaddressFragment != null) {
            fragmentTransaction.hide(checkoutShippingSelectaddressFragment);
        }
        if (checkoutPaymentFragment != null) {
            fragmentTransaction.hide(checkoutPaymentFragment);
        }
        if (checkoutReviewFragment != null) {
            fragmentTransaction.hide(checkoutReviewFragment);
        }
    }

    public void openSelectFragment() {
        fragmentTransaction = getFragmentManager().beginTransaction();
        if (checkoutShippingSelectaddressFragment != null) {
            hiddenAll();
        } else {
            checkoutShippingSelectaddressFragment = new CheckoutShippingSelectaddressFragment();
            fragmentTransaction.add(R.id.ll_checkout_body, checkoutShippingSelectaddressFragment, "selectAddressFragment");
        }
        fragmentTransaction.show(checkoutShippingSelectaddressFragment).commitAllowingStateLoss();
        list_fragment_shipping.add(checkoutShippingSelectaddressFragment);
        list_fragment.add(checkoutShippingSelectaddressFragment);
        addressConditionInShipping = "1";
    }

    private void initView() {
        mImageLoader = new ImageLoader(this);
        animationSet = new AnimationSet(true);
        tvMenuPayment = (TextView) findViewById(R.id.tv_checkout_menu_payment);
        tvMenuReview = (TextView) findViewById(R.id.tv_checkout_menu_review);
        tvMenuShipping = (TextView) findViewById(R.id.tv_checkout_menu_shipping);
        tvSliderFirst = (TextView) findViewById(R.id.tv_checkout_slider_first);
        tvSliderSecond = (TextView) findViewById(R.id.tv_checkout_slider_second);
        tvSliderThird = (TextView) findViewById(R.id.tv_checkout_slider_third);
        ivArrow = (ImageView) findViewById(R.id.iv_checkout_arrow);
        btnContinue = (TextView) findViewById(R.id.btn_checkout_payment_continue);
        ll_btn = (LinearLayout) findViewById(R.id.ll_checkout_bottomBar);

        scrollViewBody = (ScrollView) findViewById(R.id.sv_checkout_body);
        llBody = (LinearLayout) findViewById(R.id.ll_checkout_body);
        progressBarLoading = (ProgressBar) findViewById(R.id.pb_checkout_body_loading);
        tvMenuShipping.setTextColor(getResources().getColor(R.color.purple));
        btnContinue.setOnClickListener(this);
        ll_btn.setOnClickListener(this);
        ll_btn.setVisibility(View.VISIBLE);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_checkout_payment_continue:
                continueOption();
                break;
            default:
                break;
        }
    }

    /**
     * GO BACK BUTTON
     * go back to last fragment or activity
     */
    private void goBackOption() {
        /**
         * if it is in shipping page,
         * need to record all of ever option fragment in shipping page
         * and go back to relative and reseasonable fragment.
         */
        if (list_fragment.size() == 1) {
            neverEnterIntoNext = true;
        }
        isGoBack = true;
        /**
         *thinking train:(there are three fragments in shipping module, one in payment ,one in review)
         * condition 1: operate in shipping module and never enter into next module
         *              The "go back" operation should be less than three times,then go back to last activity
         * condition 2: operate in payment or review module
         *              go back to each last fragment in turn,when turn to shipping module ,show it's last fragment
         */
        setButtonEnable(true);
        //condition1:
        if (list_fragment.size() == 1 && neverEnterIntoNext) {
            InputMethodManager inputMethodManager = (InputMethodManager) CheckoutActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
            inputMethodManager.hideSoftInputFromWindow(llBody.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            if (list_fragment_shipping.size() - 1 < 0) {
                return;
            }
            Fragment currentFragmentInShipping = list_fragment_shipping.get(list_fragment_shipping.size() - 1);
            list_fragment_shipping.remove(list_fragment_shipping.size() - 1);

            if (list_fragment_shipping.size() == 0) {
                onBackPressed();
            } else {
                Fragment fragmentOfGoingTo = list_fragment_shipping.get(list_fragment_shipping.size() - 1);
                list_fragment.clear();//Only One address type fragment is permit in list_fragment
                list_fragment.add(fragmentOfGoingTo);

                /**
                 * find current fragment according to if it is CheckoutShippingAddaddressFragment or not
                 */
                if (fragmentOfGoingTo instanceof CheckoutShippingAddaddressFragment) {
                    addressConditionInShipping = "0";
                    fragmentTransaction = getFragmentManager().beginTransaction();
                    fragmentTransaction.setCustomAnimations(
                            R.animator.fragment_slide_left_enter,
                            R.animator.fragment_slide_right_exit
                    )/*.add(R.id.ll_checkout_body, fragmentOfGoingTo, "addNewAddressFragment")*/;
                    fragmentTransaction.remove(currentFragmentInShipping);
                    fragmentTransaction.show(fragmentOfGoingTo).commitAllowingStateLoss();

                } else {
                    /**
                     * If go back go select address fragment , need to refresh
                     */
                    if (fragmentOfGoingTo instanceof CheckoutShippingSelectaddressFragment) {
                        ((CheckoutShippingSelectaddressFragment) fragmentOfGoingTo).sendRequestToGetSelectAddress();
                    }

                    addressConditionInShipping = "1";

                    fragmentTransaction = getFragmentManager().beginTransaction();
                    fragmentTransaction.setCustomAnimations(
                            R.animator.fragment_slide_left_enter,
                            R.animator.fragment_slide_right_exit
                    )/*.add(R.id.ll_checkout_body, fragmentOfGoingTo, "defaultaddressFragment")*/;
                    fragmentTransaction.remove(currentFragmentInShipping);
                    fragmentTransaction.show(fragmentOfGoingTo).commitAllowingStateLoss();
                    CheckoutShippingSelectaddressFragment checkoutShippingSelectaddressFragment = (CheckoutShippingSelectaddressFragment) fragmentOfGoingTo;
                    checkoutShippingSelectaddressFragment.btnAddNewAddress.setVisibility(View.VISIBLE);
                }
            }
        } else {
            //condition2:
            //step1: remove the current fragment
            Fragment currentFragment = list_fragment.get(list_fragment.size() - 1);
            if (list_fragment.size() > 0) {
                list_fragment.remove(list_fragment.size() - 1);
            }
            //step2: judge the count of list_fragment
            if (list_fragment.size() == 0) {
                onBackPressed();
            } else {
                scrollViewBody.scrollTo(0, 0);
                //step3: find the last fragment to show
                Fragment fragmentOfGoingTo = list_fragment.get(list_fragment.size() - 1);
//                String fragmentName = null;
//                if (list_fragment.size() == 3) {
//                    fragmentName = "reviewFragment";
//                } else if (list_fragment.size() == 2) {
//                    fragmentName = "paymentFragment";
//                } else if (list_fragment.size() == 1) {
//                    fragmentName = "defaultaddressFragment";
//                } else {
//                    Toast.makeText(CheckoutActivity.this, "No more fragement now!", Toast.LENGTH_SHORT).show();
//                }
                fragmentTransaction = getFragmentManager().beginTransaction();
                /**
                 * In order to keep payment information, can't remove but hide.
                 */
                if (currentFragment instanceof CheckoutPaymentFragment) {
                    currentModule -= 1;
                    fragmentTransaction.hide(currentFragment);
                    JLogUtils.i("zzz", "back--->111");

                } else if (currentFragment instanceof CheckoutReviewFragment) {
                    JLogUtils.i("zzz", "back--->222");
                    if (skipPayment == 1) {//means grand total is 0, should jump from 3 to 1, review order to shipping address.
                        JLogUtils.i("zzz", "back--->222_111");
                        currentModule -= 2;
                    } else {
                        JLogUtils.i("zzz", "back--->222_222");
                        currentModule -= 1;
                        Fragment beforeFragment = list_fragment.get(list_fragment.size() - 1);
                        if (beforeFragment instanceof CheckoutPaymentFragment) {
                            ((CheckoutPaymentFragment) (beforeFragment)).sendRequestToGetPaymentList(false);
                        }
                    }
                    fragmentTransaction.hide(currentFragment);
                } else {
                    JLogUtils.i("zzz", "back--->333");
                    currentModule -= 1;
                    fragmentTransaction.remove(currentFragment);
                }
                fragmentTransaction.setCustomAnimations(R.animator.fragment_slide_left_enter, R.animator.fragment_slide_right_exit).show(fragmentOfGoingTo).commitAllowingStateLoss();

                switch (currentModule) {
                    case 2://means payment module
                        changeSliderColor(
                                getResources().getColor(R.color.grayf8f8f8),
                                getResources().getColor(R.color.purple),
                                getResources().getColor(R.color.grayf8f8f8));
                        break;
                    case 1://means shipping module
                        //close softpan
                        InputMethodManager inputMethodManager = (InputMethodManager) CheckoutActivity.this.getSystemService(Context.INPUT_METHOD_SERVICE);
                        inputMethodManager.hideSoftInputFromWindow(llBody.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);

                        changeSliderColor(
                                getResources().getColor(R.color.purple),
                                getResources().getColor(R.color.grayf8f8f8),
                                getResources().getColor(R.color.grayf8f8f8));

                        break;
                    default:
                        break;
                }
                //always reset bottom button
                btnContinue.setText(getResources().getString(R.string.CONTINUE));
            }
        }


    }

    /**
     * CONTINUE BUTTON
     */
    private void continueOption() {
        isGoBack = false;
        switch (currentModule) {
            case 1://In shipping Module
                sendRequestAndGoToNextPaymentModule();
//                GaTrackHelper.getInstance().googleAnalytics("Select Address Screen", this);
//                JLogUtils.i("googleGA_screen", "Select Address Screen");
                break;
            case 2://In payment Module
                sendRequestToSavePayment();
//                GaTrackHelper.getInstance().googleAnalytics("Select Payment Screen", this);
//                JLogUtils.i("googleGA_screen", "Select Payment Screen");
                break;
            case 3://place my order
                mGATrackPlaceOrderToResultTimeStart = GaTrackHelper.getInstance().googleAnalyticsTimeStart();
                IsOldVersion();

//                try {
//                    GaTrackHelper.getInstance().googleAnalytics("Review Order Screen ", this);
////                    JLogUtils.i("CheckoutActivity","paymentSaveReturnEntity:"+paymentSaveReturnEntity.getDiscount());
////                    JLogUtils.i("CheckoutActivity","paymentSaveReturnEntity:"+paymentSaveReturnEntity.getShipping());
//                    String discount = "";
//                    String shippingFee = "";
//                    if (paymentSaveReturnEntity.getDiscount() != null) {
//                        discount = paymentSaveReturnEntity.getDiscount().get("value");
//                    }
//                    if (paymentSaveReturnEntity.getShipping() != null) {
//                        shippingFee = paymentSaveReturnEntity.getShipping().get("value");
//                    }
////                    FirebaseEventUtils.getInstance().customizedBeginCheck(CheckoutActivity.this, discount, JDataUtils.formatDouble(paymentSaveReturnEntity.getGrandtotal()), JDataUtils.formatDouble(shippingFee));
//                } catch (Exception ex) {
//                    ex.getStackTrace();
//                }
//                JLogUtils.i("googleGA_screen", "Review Order Screen ");
                break;
        }
    }

    private void PlaceMyOrder() {
        mCheckoutDao.savePlaceOrder(GemfiveApplication.getAppConfiguration().getUserInfo(this).getSessionKey());
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mwebView != null) {
            mwebView.setVisibility(View.GONE);
        }
    }

    /**
     * Go to next payment redirect activity ,then open a webview and send Request Of Payment Redirect
     *
     * @param lastrealorderid
     * @param payment_type
     */
    private void goToPaymentRedirectPage(String lastrealorderid, String payment_type, String grandTotal, String shippingFee, String payUrl) {

        //Go to next payment redirect activity ,then open a webview and send Request Of Payment Redirect

        Bundle bundle = new Bundle();

        bundle.putLong("mGATrackTimeStart", mGATrackPlaceOrderToResultTimeStart);

        bundle.putString("session_key", GemfiveApplication.getAppConfiguration().getUserInfo(this).getSessionKey());
        bundle.putString("lastrealorderid", lastrealorderid);
        bundle.putString("payment_type", payment_type);
        bundle.putString("grand_total", grandTotal.replace("RM", "").trim());
        bundle.putString("shipping_fee", shippingFee.replace("RM", "").trim());
        bundle.putInt("paymentMethod", CheckoutPaymentRedirectActivity.PAYMENT_CARD);
        if (!TextUtils.isEmpty(payUrl)) {
            bundle.putString("payUrl", payUrl);
        }
        bundle.putSerializable("paymentSaveReturnEntity", paymentSaveReturnEntity);
        bundle.putString("payUrl", payUrl);
        bundle.putInt("fromType", fromType);
        if (mDiscountBean != null) {
            bundle.putSerializable("discountBean", mDiscountBean);
        }

        startNextActivity(bundle, CheckoutPaymentRedirectActivity.class, true);
    }

    public void goToPaymentSuccess(String lastrealorderid, String payment_type, String grandTotal, String shippingFee, String html) {
        Bundle bundle = new Bundle();
        bundle.putString("payment_status", "1");
        bundle.putString("orderNumber", lastrealorderid);
        bundle.putString("grand_total", grandTotal);
        bundle.putString("shipping_fee", shippingFee);
        bundle.putString("html", html);
        if (mDiscountBean != null) {
            bundle.putSerializable("discountBean", mDiscountBean);
        }
        bundle.putInt("fromType", fromType);
        bundle.putLong("mGATrackTimeStart", mGATrackPlaceOrderToResultTimeStart);
        bundle.putSerializable("paymentSaveReturnEntity", paymentSaveReturnEntity);
        startNextActivity(bundle, CheckoutPaymentStatusActivity.class, true);
    }


    public void gaTrackerPlaceOrder() {
//        try {
//            GaTrackHelper.getInstance().googleAnalyticsEvent("Checkout Action",
//                    "Place Order",
//                    null,
//                    null);
//
//        } catch (Exception ex) {
//            ex.getStackTrace();
//        }

    }

    /**
     * transfer ChannelCode according to channel text
     *
     * @param text
     * @return
     */
    private String transferChannelCode(String text) {
        String channelCode;
        if ("Maybank2u".equalsIgnoreCase(text)) {
            channelCode = "maybank2u";
        } else if ("Hong Leong Connect".equalsIgnoreCase(text)) {
            channelCode = "hlb";
        } else if ("CIMB Clicks".equalsIgnoreCase(text)) {
            channelCode = "cimb";
        } else if ("RHB Now".equalsIgnoreCase(text)) {
            channelCode = "rhb";
        } else if ("AmOnline".equalsIgnoreCase(text)) {
            channelCode = "amb";
        } else if ("FPX e-Payment Channel".equalsIgnoreCase(text)) {
            channelCode = "fpx";
        } else {
            channelCode = null;
        }

        return channelCode;
    }

    /**
     * send Request To Save Payment and parse return entity which is param in next review fragment
     */
    private void sendRequestToSavePayment() {
        final CheckoutPaymentFragment paymentFragment = (CheckoutPaymentFragment) getFragmentManager().findFragmentByTag("paymentFragment");
        paymentFragment.savePayment(null);
    }

    public void setButtonEnable(boolean enable) {
        btnContinue.setEnabled(enable);
        if (enable) {
            btnContinue.setBackgroundResource(R.drawable.big_button_style_purple);
        } else {
            btnContinue.setBackgroundResource(R.drawable.big_button_style_b8);
        }
    }

    public String paymentMethodCode;
    public String molpayType;
    private int paymethodType;//1 online 2credit 3.offline payment
    private String bank;

    public void switReviewFragment(String molpayType, CheckoutPaymentSaveReturnEntity paymentSaveReturnEntity, String code, String html, int type, String bank) {
        this.html = html;
        this.paymentMethodCode = code;
        paymethodType = type;
        this.paymentSaveReturnEntity = paymentSaveReturnEntity;
        changeSliderColor(
                getResources().getColor(R.color.grayf8f8f8),
                getResources().getColor(R.color.grayf8f8f8),
                getResources().getColor(R.color.purple));
        //switch fragment and set params
        checkoutReviewFragment = new CheckoutReviewFragment();
        Bundle bundle = new Bundle();
        this.molpayType = molpayType;
        bundle.putSerializable("paymentSaveReturnEntity", paymentSaveReturnEntity);
        this.bank = bank;
//        if (parameters.getUrlParams().get("payment[molpay_type]") != null) {
//            bundle.putString("payment[molpay_type]", parameters.getUrlParams().get("payment[molpay_type]"));
//        }
        if (!TextUtils.isEmpty(molpayType)) {
            bundle.putString("payment[molpay_type]", molpayType);
        }
        if (!TextUtils.isEmpty(code)) {
            bundle.putString("code", code);
        }
        checkoutReviewFragment.setArguments(bundle);
        fragmentTransaction = getFragmentManager().beginTransaction();
        currentModule = 3;
        btnContinue.setText(getResources().getString(R.string.PLACE_MY_ORDER));
        //record this fragment
        list_fragment.add(checkoutReviewFragment);
        fragmentTransaction.add(R.id.ll_checkout_body, checkoutReviewFragment, "reviewFragment");
        fragmentTransaction.hide(list_fragment.get(1));
        fragmentTransaction.setCustomAnimations(
                R.animator.fragment_slide_right_enter,
                R.animator.fragment_slide_left_exit
        ).show(checkoutReviewFragment).commitAllowingStateLoss();
    }


    public void showProductDialog(String title, ArrayList<DialogProductBean> beans) {
        // The following item(s) in your cart cannot be delivered to your location
        final MaterialDialog mMaterialDialog = new MaterialDialog(CheckoutActivity.this);
        mMaterialDialog.setTitle(title);
        View view = LayoutInflater.from(CheckoutActivity.this).inflate(R.layout.dialog_list, null);
        ListView listView = (ListView) view.findViewById(R.id.listview);
        DialogProductAdapter adapter = new DialogProductAdapter(CheckoutActivity.this, beans, mImageLoader);
        listView.setAdapter(adapter);
        mMaterialDialog.setContentView(view);
        mMaterialDialog.setPositiveButton("OK", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMaterialDialog.dismiss();
            }
        });
        mMaterialDialog.show();
    }

    private AddressParameter addressParams;

    /**
     * send Request And Go To Next Payment Module
     */
    private void sendRequestAndGoToNextPaymentModule() {
        mGATrackAddressToPaymentTimeEnable = true;
        mGATrackAddressToPaymentTimeStart = GaTrackHelper.getInstance().googleAnalyticsTimeStart();
//        SVRParameters parameters = new SVRParameters();
//        parameters.put("session_key",  GemfiveApplication.getAppConfiguration().getUserInfo(this).getSessionKey());
        //judge to show which fragment(module)
        switch (Integer.parseInt(addressConditionInShipping)) {

            case 0://It is adding new address or editing address,connect newAddressWebservice or editAddressWebService
                //we need to get params from addNewAddressFragment first.
                final CheckoutShippingAddaddressFragment addNewAddressFragment = (CheckoutShippingAddaddressFragment) getFragmentManager().findFragmentByTag("addNewAddressFragment");
                final CheckoutShippingAddaddressFragment editAddressFragment = (CheckoutShippingAddaddressFragment) getFragmentManager().findFragmentByTag("editAddressFragment");
                if (addNewAddressFragment != null) {
                    //then validate these params
                    addressParams = validateDatasOfNewAddressFragment(addNewAddressFragment, null);
                    if (addressParams == null) {//do not pass validation
                        return;
                    }
                    setButtonEnable(false);
                    addNewAddressFragment.tvErrorMsg.setText("");
                    //after validation:
                    mDialog = JViewUtils.showProgressDialog(CheckoutActivity.this);
                    /**
                     * step1, send request to save address
                     */
                    mAccountDao.addressSave(GemfiveApplication.getAppConfiguration().getUserInfo(this).getSessionKey(),
                            addressParams.getFirstname(), addressParams.getLastname(), addressParams.getCountryId(), addressParams.getTelephone(), addressParams.getStreet0(),
                            addressParams.getStreet1(), addressParams.getPostcode(), addressParams.getCity(), addressParams.getRegion(), addressParams.getRegionId(), "0");
                } else if (editAddressFragment != null && editAddressFragment.AllVerifyNotNull()) {
                    //then validate these params
                    addressParams = validateDatasOfNewAddressFragment(null, editAddressFragment);
//                    String errorMsg = parameters.getUrlParams().get("validation_notpass_reason");

                    if (addressParams == null) {//do not pass validation
                        // jk
                        return;
                    }
                    addressParams.setAddressId(editAddressFragment.tvPhone_otheruse.getHint().toString());

                    mDialog = JViewUtils.showProgressDialog(CheckoutActivity.this);
                    setButtonEnable(false);
                    editAddressFragment.tvErrorMsg.setText("");
                    mAccountDao.EditSave(addressParams.getAddressId(), GemfiveApplication.getAppConfiguration().getUserInfo(this).getSessionKey(), addressParams.getFirstname(),
                            addressParams.getLastname(), addressParams.getCountryId(), addressParams.getTelephone(), addressParams.getStreet0(), addressParams.getStreet1(),
                            addressParams.getPostcode(), addressParams.getCity(), addressParams.getRegion(), addressParams.getRegionId(), null);
                } else {
                    if (editAddressFragment.AllVerifyNotNull()) {
                        Toast.makeText(CheckoutActivity.this, "failed to find and cast to Add new Address Fragment or edit address fragment", Toast.LENGTH_SHORT).show();
                    }
                }
                break;

            case 1://It's in shipping select or default address module,
                try {
                    final Fragment fragment = getFragmentManager().findFragmentByTag("defaultaddressFragment");
//                    final CheckoutShippingDefaultaddressFragment defaultaddressFragment = (CheckoutShippingDefaultaddressFragment) getFragmentManager().findFragmentByTag("defaultaddressFragment");
                    final CheckoutShippingSelectaddressFragment selectaddressFragment;
                    Fragment defaultOrSelectFragment;
                    CheckoutShippingSelectaddressFragment selectFragment = null;
                    JLogUtils.i("checkout-select-address-fragment:", "do not permit to continue");
                    CheckoutDefaultShippingAddress defaultShippingAddress = null;
                    if (fragment != null) {//maybe in select address fragment
                        if (fragment instanceof CheckoutShippingDefaultaddressFragment) {
                            CheckoutShippingDefaultaddressFragment defaultaddressFragment = (CheckoutShippingDefaultaddressFragment) fragment;
                            defaultShippingAddress = defaultaddressFragment.address;
                        }
                    } else {
                        Fragment fragment1 = getFragmentManager().findFragmentByTag("selectAddressFragment");
                        selectFragment = (CheckoutShippingSelectaddressFragment) fragment1;
                        defaultShippingAddress = selectFragment.mAddress;
                        if (defaultShippingAddress == null) {
                            Toast.makeText(CheckoutActivity.this, R.string.select_address, Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    addressParams = new AddressParameter();
                    addressParams.setAddressId(defaultShippingAddress.getAddressId());
                    addressParams.setFirstname(defaultShippingAddress.getFirstName());
                    addressParams.setLastname(defaultShippingAddress.getLastName());
                    addressParams.setCountryId(defaultShippingAddress.getCountryId());
                    addressParams.setStreet0(defaultShippingAddress.getStreet().get(0));
                    addressParams.setStreet1(defaultShippingAddress.getStreet().get(1));
                    addressParams.setPostcode(defaultShippingAddress.getPostcode());
                    addressParams.setCity(defaultShippingAddress.getCity());
                    addressParams.setRegionId(defaultShippingAddress.getRegionId());
                    addressParams.setRegion(defaultShippingAddress.getRegion());
                    addressParams.setTelephone(defaultShippingAddress.getTelephone());
                    mDialog = JViewUtils.showProgressDialog(CheckoutActivity.this);
                    setButtonEnable(false);
                    mCheckoutDao.saveBilling(GemfiveApplication.getAppConfiguration().getUserInfo(this).getSessionKey(), addressParams);
                } catch (ClassCastException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }

    private static class DataHandler extends Handler {
        private final WeakReference<CheckoutActivity> mActivity;

        public DataHandler(CheckoutActivity activity) {
            mActivity = new WeakReference<CheckoutActivity>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            if (mActivity.get() == null) {
                return;
            }
            switch (msg.what) {
                case ProductDao.REQUEST_CHECKVERSION:
                    if (msg.arg1 == ProductDao.RESPONSE_SUCCESS) {
                        mActivity.get().PlaceMyOrder();
                    } else {
                        if (mActivity.get().mCheckoutPaymentDialog != null) {
                            mActivity.get().mCheckoutPaymentDialog.dismiss();
                        }
                        mActivity.get().setButtonEnable(true);
                        String title = mActivity.get().getResources().getString(R.string.versionCheckTitle);
                        String hintmsg = mActivity.get().getResources().getString(R.string.versionCheckMsg);
                        String btnMsg = mActivity.get().getResources().getString(R.string.update);
                        JViewUtils.showMaterialDialog(mActivity.get(), title, hintmsg, btnMsg, mActivity.get().updateListener, false);
                    }
                    break;
                case MyAccountDao.REQUEST_ADDRESS_SAVE:
                    if (msg.arg1 == MyAccountDao.RESPONSE_SUCCESS) {
                        SVRAddAddress address = (SVRAddAddress) msg.obj;
                        mActivity.get().addressParams.setAddressId(address.getId());
                        mActivity.get().mCheckoutDao.saveBilling(GemfiveApplication.getAppConfiguration().getUserInfo(mActivity.get()).getSessionKey(), mActivity.get().addressParams);
                    } else {
                        if (mActivity.get().mDialog != null) {
                            mActivity.get().mDialog.cancel();
                        }
                        String faildStr = (String) msg.obj;
                        mActivity.get().setButtonEnable(true);
                        if (faildStr != null && !JToolUtils.expireHandler(mActivity.get(), faildStr, mActivity.get().REQUESTCODE_LOGIN)) {
                            CheckoutShippingAddaddressFragment addNewAddressFragment = (CheckoutShippingAddaddressFragment) mActivity.get().getFragmentManager().findFragmentByTag("addNewAddressFragment");
                            addNewAddressFragment.tvErrorMsg.setText(faildStr);
                            addNewAddressFragment.tvErrorMsg.setVisibility(View.VISIBLE);
                            addNewAddressFragment.tvErrorMsg.setFocusable(true);
                            addNewAddressFragment.tvErrorMsg.setFocusableInTouchMode(true);
                            addNewAddressFragment.tvErrorMsg.requestFocus();
                        }
                    }
                    break;

                case CheckoutDao.REQUEST_SAVEBILLING:
                    if (mActivity.get().mDialog != null) {
                        mActivity.get().mDialog.cancel();
                    }
                    if (msg.arg1 == CheckoutDao.RESPONSE_SUCCESS) {
                        mActivity.get().selectedAddressTrack();
                        mActivity.get().setButtonEnable(true);
                        SVRAppserviceSaveBillingEntity saveBillingEntity = (SVRAppserviceSaveBillingEntity) msg.obj;
                        //handler sku
                        if (saveBillingEntity.getError_products() != null && saveBillingEntity.getError_products().size() > 0) {
                            mActivity.get().showProductDialog(mActivity.get().errorProductTitle, saveBillingEntity.getError_products());
                            return;
                        }
                        mActivity.get().gaTrackerSaveShipping(mActivity.get().addressParams.getRegion());
                        if (1 == saveBillingEntity.getSkip_payment()) {
                            /**
                             * grand total is 0 and redirect to review order fragment.
                             */
                            //零元购
                            mActivity.get().skipPayment = 1;
                            mActivity.get().changeSliderColor(
                                    mActivity.get().getResources().getColor(R.color.grayf8f8f8),
                                    mActivity.get().getResources().getColor(R.color.grayf8f8f8),
                                    mActivity.get().getResources().getColor(R.color.purple));
                            //switch fragment and set params
                            mActivity.get().checkoutReviewFragment = new CheckoutReviewFragment();
                            Bundle bundle = new Bundle();
                            bundle.putSerializable("paymentSaveReturnEntity", saveBillingEntity);
                            mActivity.get().checkoutReviewFragment.setArguments(bundle);
                            mActivity.get().fragmentTransaction = mActivity.get().getFragmentManager().beginTransaction();
                            //change currentFragment(module)
                            mActivity.get().currentModule = 3;
                            //change button text
                            mActivity.get().btnContinue.setText(mActivity.get().getResources().getString(R.string.PLACE_MY_ORDER));
                            //record this fragment
                            mActivity.get().list_fragment.add(mActivity.get().checkoutReviewFragment);
                            mActivity.get().fragmentTransaction.add(R.id.ll_checkout_body, mActivity.get().checkoutReviewFragment, "reviewFragment");
                            mActivity.get().fragmentTransaction.hide(mActivity.get().list_fragment.get(0));
                            mActivity.get().fragmentTransaction.setCustomAnimations(
                                    R.animator.fragment_slide_right_enter,
                                    R.animator.fragment_slide_left_exit
                            ).show(mActivity.get().checkoutReviewFragment).commitAllowingStateLoss();
                        } else {
                            /**
                             * normal payment
                             */
                            mActivity.get().skipPayment = 0;
                            mActivity.get().changeSliderColor(mActivity.get().getResources().getColor(R.color.grayf8f8f8), mActivity.get().getResources().getColor(R.color.purple), mActivity.get().getResources().getColor(R.color.grayf8f8f8));
                            //switch fragment
                            mActivity.get().fragmentTransaction = mActivity.get().getFragmentManager().beginTransaction();
                            mActivity.get().checkoutPaymentFragment = mActivity.get().getFragmentManager().findFragmentByTag("paymentFragment");
                            if (mActivity.get().checkoutPaymentFragment == null) {
                                mActivity.get().checkoutPaymentFragment = new CheckoutPaymentFragment();
                                mActivity.get().fragmentTransaction.add(R.id.ll_checkout_body, mActivity.get().checkoutPaymentFragment, "paymentFragment");
                                mActivity.get().fragmentTransaction.hide(mActivity.get().list_fragment.get(0));
                                mActivity.get().fragmentTransaction.setCustomAnimations(
                                        R.animator.fragment_slide_right_enter,
                                        R.animator.fragment_slide_left_exit
                                ).show(mActivity.get().checkoutPaymentFragment).commitAllowingStateLoss();
                            } else {
                                mActivity.get().fragmentTransaction.hide(mActivity.get().list_fragment.get(0));
                                mActivity.get().fragmentTransaction.setCustomAnimations(
                                        R.animator.fragment_slide_right_enter,
                                        R.animator.fragment_slide_left_exit
                                ).show(mActivity.get().checkoutPaymentFragment).commitAllowingStateLoss();
                                ((CheckoutPaymentFragment) mActivity.get().checkoutPaymentFragment).sendRequestToGetPaymentList(true);
                            }
                            mActivity.get().list_fragment.add(mActivity.get().checkoutPaymentFragment);
                            //change currentFragment(module)
                            mActivity.get().currentModule = 2;
                        }
                        mActivity.get().neverEnterIntoNext = false;
                        mActivity.get().scrollViewBody.scrollTo(0, 0);
//                        try {
//                            String CustomerId = GemfiveApplication.getAppConfiguration().getUser().getId();
//                            GaTrackHelper.getInstance().googleAnalyticsEvent("Checkout Action",
//                                    "Save Address",
//                                    "Shipping State",
//                                    Long.valueOf(CustomerId));
////                            JLogUtils.i("googleGA", "checkout shipping  点击 continue");
//                        } catch (NumberFormatException e) {
//                            e.printStackTrace();
//                        }

                    } else {
                        String faildStr = (String) msg.obj;
                        mActivity.get().setButtonEnable(true);
                        if (faildStr != null && !JToolUtils.expireHandler(mActivity.get(), faildStr, mActivity.get().REQUESTCODE_LOGIN)) {
                            CheckoutShippingAddaddressFragment addNewAddressFragment = (CheckoutShippingAddaddressFragment) mActivity.get().getFragmentManager().findFragmentByTag("addNewAddressFragment");
                            if (addNewAddressFragment != null) {
                                addNewAddressFragment.tvErrorMsg.setText(faildStr);
                                addNewAddressFragment.tvErrorMsg.setVisibility(View.VISIBLE);
                                addNewAddressFragment.tvErrorMsg.setFocusable(true);
                                addNewAddressFragment.tvErrorMsg.setFocusableInTouchMode(true);
                                addNewAddressFragment.tvErrorMsg.requestFocus();
                            } else {
                                CheckoutShippingSelectaddressFragment selectFragment = (CheckoutShippingSelectaddressFragment) mActivity.get().getFragmentManager().findFragmentByTag("selectAddressFragment");
                                selectFragment.tvErrorMsg.setText(faildStr);
                                selectFragment.tvErrorMsg.setVisibility(View.VISIBLE);
                                selectFragment.tvErrorMsg.setFocusable(true);
                                selectFragment.tvErrorMsg.setFocusableInTouchMode(true);
                                selectFragment.tvErrorMsg.requestFocus();
                            }
                        }
                    }
                    break;
                case MyAccountDao.REQUEST_EDIT_SAVE:
                    if (msg.arg1 == MyAccountDao.RESPONSE_SUCCESS) {
                        mActivity.get().mCheckoutDao.saveBilling(GemfiveApplication.getAppConfiguration().getUserInfo(mActivity.get()).getSessionKey(), mActivity.get().addressParams);
                    } else {
                        if (mActivity.get().mDialog != null) {
                            mActivity.get().mDialog.cancel();
                        }
                        mActivity.get().setButtonEnable(true);
                        String faildStr = (String) msg.obj;
                        if (faildStr != null && !JToolUtils.expireHandler(mActivity.get(), faildStr, mActivity.get().REQUESTCODE_LOGIN)) {
                            CheckoutShippingAddaddressFragment editAddressFragment = (CheckoutShippingAddaddressFragment) mActivity.get().getFragmentManager().findFragmentByTag("editAddressFragment");
                            editAddressFragment.tvErrorMsg.setText(faildStr);
                            editAddressFragment.tvErrorMsg.setVisibility(View.VISIBLE);
                            editAddressFragment.tvErrorMsg.setFocusable(true);
                            editAddressFragment.tvErrorMsg.setFocusableInTouchMode(true);
                            editAddressFragment.tvErrorMsg.requestFocus();
                        }
                    }
                    break;
                case CheckoutDao.REQUEST_PLACEORDER:
                    mActivity.get().setButtonEnable(true);
                    if (msg.arg1 == CheckoutDao.RESPONSE_SUCCESS) {
                        mActivity.get().closeCheckoutPaymentDialog();
                        mActivity.get().saveShoppingCartCount(0);
                        CheckoutReviewFragment reviewFragment = (CheckoutReviewFragment) mActivity.get().getFragmentManager().findFragmentByTag("reviewFragment");
                        SVRAppserviceSaveOrderReturnEntity saveOrderReturnEntity = (SVRAppserviceSaveOrderReturnEntity) msg.obj;
                        String grandTotal = reviewFragment.tvGrandTotal.getText().toString();
                        String shippingFee = reviewFragment.tvShippingfee.getText().toString();
                        mActivity.get().paymentSaveReturnEntity = reviewFragment.getPaymentSaveReturnEntity();
                        //getResources().getString(R.string.payment_method_ONLINE_BANKING)
                        if (mActivity.get().paymethodType == CheckoutPaymentFragment.ONLINEPAYMENT) {
                            /**
                             * Online Banking
                             */
//                            grandTotal = grandTotal.substring(3, grandTotal.length());
                            shippingFee = shippingFee.substring(3, shippingFee.length());
                            String phone = reviewFragment.address.getTelephone();
                            String channelCode = mActivity.get().transferChannelCode(mActivity.get().bank);
                            if (channelCode == null) {
                                reviewFragment.tvErrorMsg.setText(mActivity.get().getResources().getString(R.string.Payment_channel_is_incorrect));
                                reviewFragment.tvErrorMsg.setVisibility(View.VISIBLE);
                            } else {
                                mActivity.get().goToMolpayPage(saveOrderReturnEntity.getLastrealorderid(), saveOrderReturnEntity.getAmount(), shippingFee, phone, channelCode, reviewFragment.productName);
                            }
                        } else {

                            //offline payment
                            if (mActivity.get().paymethodType == CheckoutPaymentFragment.OFFLINEPAYMENT) {
                                mActivity.get().goToPaymentSuccess(saveOrderReturnEntity.getLastrealorderid(), reviewFragment.payment_type, grandTotal, shippingFee, mActivity.get().html);
                            } else if (mActivity.get().paymethodType == CheckoutPaymentFragment.CODPAYMENT) {

                                mActivity.get().goToPaymentSuccess(saveOrderReturnEntity.getLastrealorderid(), reviewFragment.payment_type, grandTotal, shippingFee, saveOrderReturnEntity.getCashondeliveryContent());
                            } else {
//                          * Credit Card
                                mActivity.get().goToPaymentRedirectPage(saveOrderReturnEntity.getLastrealorderid(), reviewFragment.payment_type, grandTotal, shippingFee, saveOrderReturnEntity.getPaymentUrl());
                            }
                        }
                        mActivity.get().gaTrackerPlaceOrder();
                    } else {
                        String errorStr = (String) msg.obj;
                        mActivity.get().closeCheckoutPaymentDialog();
                        if (errorStr != null && !JToolUtils.expireHandler(mActivity.get(), errorStr, mActivity.get().REQUESTCODE_LOGIN)) {
                            JViewUtils.showMaterialDialog(mActivity.get(), "", errorStr, null);
                        }
                    }
                    break;
                case CheckoutDao.REQUEST_CHANGEORDERSTATUS:
                    mActivity.get().closeCheckoutPaymentDialog();
                    if (msg.arg1 == CheckoutDao.RESPONSE_SUCCESS) {
                        Bundle bundle_success = new Bundle();
                        bundle_success.putLong("mGATrackTimeStart", mActivity.get().mGATrackPlaceOrderToResultTimeStart);
                        bundle_success.putString("payment_status", "1");
                        bundle_success.putString("lastrealorderid", mActivity.get().order_id);
                        bundle_success.putString("grand_total", mActivity.get().amount);
                        bundle_success.putString("shipping_fee", mActivity.get().shippingFee);
                        bundle_success.putSerializable("paymentSaveReturnEntity", mActivity.get().paymentSaveReturnEntity);
                        bundle_success.putInt("fromType", mActivity.get().fromType);
                        bundle_success.putInt("paymentMethod", CheckoutPaymentRedirectActivity.PAYMENT_ONLINE);
                        if (mActivity.get().mDiscountBean != null) {
                            bundle_success.putSerializable("discountBean", mActivity.get().mDiscountBean);
                        }
                        mActivity.get().startNextActivity(bundle_success, CheckoutPaymentRedirectActivity.class, true);
                    } else {
                        String errorMsg = (String) msg.obj;
                        if (!JToolUtils.expireHandler(mActivity.get(), errorMsg, mActivity.get().REQUESTCODE_LOGIN)) {
                            Bundle bundle_failuer = new Bundle();
                            bundle_failuer.putString("payment_status", "0");
                            bundle_failuer.putString("errorMsg", errorMsg);
                            bundle_failuer.putString("orderNumber", mActivity.get().order_id);
                            if (mActivity.get().mDiscountBean != null) {
                                bundle_failuer.putSerializable("discountBean", mActivity.get().mDiscountBean);
                            }
                            bundle_failuer.putInt("fromType", mActivity.get().fromType);
                            mActivity.get().startNextActivity(bundle_failuer, CheckoutPaymentStatusActivity.class, true);
                        }
                    }
                    break;
                case ProductDao.REQUEST_ERROR:
                case MyAccountDao.ERROR:
                case CheckoutDao.REQUEST_ERROR:
                    RequestErrorHelper requestErrorHelper=new RequestErrorHelper(mActivity.get());
                    if(msg.arg1== ProductDao.REQUEST_CHECKVERSION) {
                        mActivity.get().PlaceMyOrder();
                    } else if (msg.arg1 == MyAccountDao.REQUEST_ADDRESS_SAVE || msg.arg1 == CheckoutDao.REQUEST_SAVEBILLING || msg.arg1 == MyAccountDao.REQUEST_EDIT_SAVE || msg.arg1 == CheckoutDao.REQUEST_CHANGEORDERSTATUS) {
                        if (mActivity.get().mDialog != null) {
                            mActivity.get().mDialog.cancel();
                        }
                        mActivity.get().setButtonEnable(true);
                        requestErrorHelper.showNetWorkErrorToast(msg);
                    }else if(msg.arg1==CheckoutDao.REQUEST_PLACEORDER){
                        mActivity.get().closeCheckoutPaymentDialog();
                        mActivity.get().setButtonEnable(true);
                        requestErrorHelper.showNetWorkErrorToast(msg);
                    }else if(msg.arg1==ShoppingCarDao.REQUEST_RECOVERORDER){
                        mActivity.get().closeCheckoutPaymentDialog();
                        mActivity.get().startShoppingActivity();
                    }
                    break;
                case ShoppingCarDao.REQUEST_RECOVERORDER:
                    if (mActivity.get().mDialog != null) {
                        mActivity.get().mDialog.cancel();
                    }
                    mActivity.get().closeCheckoutPaymentDialog();
                    mActivity.get().startShoppingActivity();
                    break;

            }
            super.handleMessage(msg);
        }
    }


    public void selectedAddressTrack() {
//        try {
//            FirebaseEventUtils.getInstance().customizedAddAddressInfo(this);
//        } catch (Exception ex) {
//            ex.getMessage();
//        }
    }

    public void gaTrackerSaveShipping(String state) {
//        try {
//
//            GaTrackHelper.getInstance().googleAnalyticsEvent("Checkout Action",
//                    "Save Address",
//                    state,
//                    Long.valueOf(GemfiveApplication.getAppConfiguration().getUser().getId()));
//
//        } catch (Exception ex) {
//            ex.getStackTrace();
//        }

    }

    /**
     * validate Datas Of NewAddressFragment or editAddressFragment
     *
     * @param addNewAddressFragment
     * @param editAddressFragment
     * @SVRParameters
     */
    private AddressParameter validateDatasOfNewAddressFragment(CheckoutShippingAddaddressFragment addNewAddressFragment, CheckoutShippingAddaddressFragment editAddressFragment) {

        AddressParameter params = new AddressParameter();
        if (addNewAddressFragment == null) {
            /**
             * AddNewAddress or EditAddress
             */
            addNewAddressFragment = editAddressFragment;
        }
        Editable etFirstname = addNewAddressFragment.etFirstname.getText();
        if (null == etFirstname || JDataUtils.isEmpty(etFirstname.toString().trim())) {
            addNewAddressFragment.tvFirstnameAnim.setText(getResources().getString(R.string.This_is_a_required_field));
            addNewAddressFragment.tvFirstnameAnim.setTextColor(getResources().getColor(R.color.red_common));
//            svrParameters.put("validation_notpass_reason", "firstname is required!");
            return null;
        } else {
            params.setFirstname(etFirstname.toString());
//            svrParameters.put("firstname", etFirstname.toString());
        }

        Editable etLastname = addNewAddressFragment.etLastname.getText();
        if (null == etLastname || JDataUtils.isEmpty(etLastname.toString().trim())) {
            addNewAddressFragment.tvLastnameAnim.setText(getResources().getString(R.string.This_is_a_required_field));
            addNewAddressFragment.tvLastnameAnim.setTextColor(getResources().getColor(R.color.red_common));
//            svrParameters.put("validation_notpass_reason", "lastname is required!");
            return null;
        } else {
            params.setLastname(etLastname.toString());
//            svrParameters.put("lastname", etLastname.toString());
        }

        Editable etAddressLine1 = addNewAddressFragment.etAddressLine1.getText();
        if (null == etAddressLine1 || JDataUtils.isEmpty(etAddressLine1.toString().trim())) {

            addNewAddressFragment.tvAddressLine1Anim.setText(getResources().getString(R.string.This_is_a_required_field));
            addNewAddressFragment.tvAddressLine1Anim.setTextColor(getResources().getColor(R.color.red_common));
//            svrParameters.put("validation_notpass_reason", "address line 1 is required!");

            return null;
        } else {
            params.setStreet0(etAddressLine1.toString());
//            svrParameters.put("street[0]", etAddressLine1.toString());
        }

        Editable etAddressLine2 = addNewAddressFragment.etAddressLine2.getText();
//        svrParameters.put("street[1]", etAddressLine2.toString());
        params.setStreet1(etAddressLine2.toString());
        Editable etPostCode = addNewAddressFragment.etPostCode.getText();
        if (null == etFirstname || JDataUtils.isEmpty(etPostCode.toString().trim())) {
            addNewAddressFragment.tvPostCode.setText(getResources().getString(R.string.This_is_a_required_field));
            addNewAddressFragment.tvPostCode.setTextColor(getResources().getColor(R.color.red_common));
//            svrParameters.put("validation_notpass_reason", "postcode is required!");
            return null;
        } else if (etPostCode.toString().trim().length() < 4) {
            addNewAddressFragment.tvPostCode.setText(getResources().getString(R.string.blur_postalcode));
            addNewAddressFragment.tvPostCode.setTextColor(getResources().getColor(R.color.red_common));
            return null;
        } else {
            params.setPostcode(etPostCode.toString());
//            svrParameters.put("postcode", etPostCode.toString());
        }

        Editable etShippingCity = addNewAddressFragment.etShippingCity.getText();
        if (null == etShippingCity || JDataUtils.isEmpty(etShippingCity.toString().trim())) {
            addNewAddressFragment.tvCityAnim.setText(getResources().getString(R.string.This_is_a_required_field));
            addNewAddressFragment.tvCityAnim.setTextColor(getResources().getColor(R.color.red_common));
//            svrParameters.put("validation_notpass_reason", "city is required!");
            return null;
        } else {
            params.setCity(etShippingCity.toString());
//            svrParameters.put("city", etShippingCity.toString());
        }

        Editable etPhone = addNewAddressFragment.etPhone.getText();
        if (null == etPhone || JDataUtils.isEmpty(etPhone.toString().trim())) {

            addNewAddressFragment.tvPhone.setText(getResources().getString(R.string.This_is_a_required_field));
            addNewAddressFragment.tvPhone.setTextColor(getResources().getColor(R.color.red_common));
//            svrParameters.put("validation_notpass_reason", "telephone is required!");
            return null;
        } else {
            //svrParameters.put("telephone", etPhone.toString());
            params.setTelephone(etPhone.toString());
        }

        EditText etCountry = addNewAddressFragment.etShippingCountry;
        if (etCountry.getTag() == null || JDataUtils.isEmpty(etCountry.getTag().toString())) {
//            svrParameters.put("validation_notpass_reason", "country is required!!!");
            return null;
        } else {
            params.setCountryId(etCountry.getTag().toString());
            //svrParameters.put("country_id", tvCountry.getTag().toString());
        }
        EditText etState = addNewAddressFragment.etShippingState;
        params.setRegion(etState.getText().toString());
//        svrParameters.put("region", tvState.getText().toString());
//        svrParameters.put("region_id", tvState.getTag() == null ? "" : tvState.getTag().toString());
        params.setRegionId(etState.getTag() == null ? "" : etState.getTag().toString());
        return params;

    }

    private boolean isClick = true;

    /**
     * override keyback event of device
     *
     * @param keyCode
     * @param event
     * @return
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && updateService) {
            Bundle mBundle = new Bundle();
            mBundle.putString("exitApp", "exitApp");//压入数据
            startNextActivity(mBundle, HomeActivity.class, true);
        }
        if (keyCode == KeyEvent.KEYCODE_BACK && isClick) {
            isClick = false;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isClick = true;
                }
            }, 800);
            goBackOption();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Generate a self-define dialog
     *
     * @param context
     * @param msg
     * @return Dialog
     */
    private static Dialog createLoadingDialog(Context context, String msg) {

        LayoutInflater inflater = LayoutInflater.from(context);

        View v = inflater.inflate(R.layout.dialog_loading_checkout, null);// generate view

        LinearLayout layout = (LinearLayout) v.findViewById(R.id.dialog_view);// load layout

//        ImageView spaceshipImage = (ImageView) v.findViewById(R.id.img_checkout_dialog);// ImageView

        TextView tipTextView = (TextView) v.findViewById(R.id.tv_checkout_dialog);// hint text

        // load animation
        Animation hyperspaceJumpAnimation = AnimationUtils.loadAnimation(
                context, R.anim.loading_rotate_animation);

        // use imageview to start animation
//        spaceshipImage.startAnimation(hyperspaceJumpAnimation);
        tipTextView.setText(msg);// set hint text

        Dialog loadingDialog = new Dialog(context, R.style.loading_dialog);//self define dialog

        loadingDialog.setCanceledOnTouchOutside(false);
        loadingDialog.setCancelable(false);// permit "back" to cancel dialog

        loadingDialog.setContentView(layout, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));// set layout params

        return loadingDialog;

    }

    /**
     * Translate slider as fragment changes
     */
    private void changeSliderColor(final int firstColorId, final int secondColorId, final int thirdColorId) {

        TranslateAnimation translateAnimation = null;

        //first slide
        if (firstColorId == getResources().getColor(R.color.purple)) {


            tvSliderFirst.setBackgroundColor(firstColorId);
            tvSliderSecond.setBackgroundColor(secondColorId);
            tvSliderThird.setBackgroundColor(thirdColorId);
        }

        //second slide
        if (secondColorId == getResources().getColor(R.color.purple)) {
            if (isGoBack) {

                tvSliderSecond.setBackgroundColor(secondColorId);
                tvSliderThird.setBackgroundColor(thirdColorId);

            } else {//continue operation
                tvSliderSecond.setBackgroundColor(secondColorId);
                tvSliderFirst.setBackgroundColor(firstColorId);
            }
        }

        //third slide
        if (thirdColorId == getResources().getColor(R.color.purple)) {


            tvSliderFirst.setBackgroundColor(firstColorId);
            tvSliderSecond.setBackgroundColor(secondColorId);
            tvSliderThird.setBackgroundColor(thirdColorId);
        }

        tvMenuShipping.setTextColor(firstColorId == getResources().getColor(R.color.grayf8f8f8) ? getResources().getColor(R.color.black000000) : firstColorId);
        tvMenuPayment.setTextColor(secondColorId == getResources().getColor(R.color.grayf8f8f8) ? getResources().getColor(R.color.black000000) : secondColorId);
        tvMenuReview.setTextColor(thirdColorId == getResources().getColor(R.color.grayf8f8f8) ? getResources().getColor(R.color.black000000) : thirdColorId);

    }

    private String html;


//    public void goToMolpayPageV2(String orderId, String amount, String shippingFee, String phone, String
//            channel, String productName){
//
//        HashMap<String, Object> paymentDetails = new HashMap<>();
//        paymentDetails.put(MOLPayActivity.mp_amount, "");
//        paymentDetails.put(MOLPayActivity.mp_username, "");
//        paymentDetails.put(MOLPayActivity.mp_password, "");
//        paymentDetails.put(MOLPayActivity.mp_merchant_ID, "");
//        paymentDetails.put(MOLPayActivity.mp_app_name, "");
//        paymentDetails.put(MOLPayActivity.mp_order_ID, "");
//        paymentDetails.put(MOLPayActivity.mp_currency, "MYR");
//        paymentDetails.put(MOLPayActivity.mp_country, "MY");
//        paymentDetails.put(MOLPayActivity.mp_verification_key, "");
//        paymentDetails.put(MOLPayActivity.mp_channel, "");
//        paymentDetails.put(MOLPayActivity.mp_bill_description, "");
//        paymentDetails.put(MOLPayActivity.mp_bill_name, "");
//        paymentDetails.put(MOLPayActivity.mp_bill_email, "");
//        paymentDetails.put(MOLPayActivity.mp_bill_mobile, "");
//        paymentDetails.put(MOLPayActivity.mp_channel_editing, false);
//        paymentDetails.put(MOLPayActivity.mp_editing_enabled, false);
//        paymentDetails.put(MOLPayActivity.mp_transaction_id, "");
//        paymentDetails.put(MOLPayActivity.mp_request_type, "");
//
//    }


    /**
     * use  molpay  xdk
     *
     * @param orderId
     * @param amount
     * @param shippingFee
     * @param phone
     * @param channel
     * @param productName
     */

    private void goToMolpayPage(String orderId, String amount, String shippingFee, String phone, String
            channel, String productName) {
        order_id = orderId;
        GOUserEntity user = GemfiveApplication.getAppConfiguration().getUserInfo(CheckoutActivity.this);

        JLocalMethod method = new JLocalMethod();
        HashMap<String, Object> paymentDetails = new HashMap<>();
        paymentDetails.put(MOLPayActivity.mp_merchant_ID, method.getMERCHANTID());
        JLogUtils.i("Pconstants", "PConstants.MP_MERCHANT_ID：" + method.getMERCHANTID());
        paymentDetails.put(MOLPayActivity.mp_app_name, method.getAPPNAME());
        JLogUtils.i("Pconstants", "PConstants.MP_APP_NAME：" + method.getAPPNAME());
        paymentDetails.put(MOLPayActivity.mp_username, method.getUSERNAME());
        JLogUtils.i("Pconstants", "PConstants.MP_USERNAME：" + method.getUSERNAME());
        paymentDetails.put(MOLPayActivity.mp_password, method.getPASSWORD());
        JLogUtils.i("Pconstants", "PConstants.MP_PASSWORD：" + method.getPASSWORD());
        paymentDetails.put(MOLPayActivity.mp_order_ID, orderId);
        JLogUtils.i("Pconstants", "PConstants.MP_ORDER_ID：" + orderId);
        paymentDetails.put(MOLPayActivity.mp_country, "MY");
        JLogUtils.i("Pconstants", "PConstants.MP_COUNTRY：MY");
        if (!TextUtils.isEmpty(amount)) {
            paymentDetails.put(MOLPayActivity.mp_amount, amount.replace(",", ""));
        }
        JLogUtils.i("Pconstants", "PConstants.MP_AMOUNT：" + Float.parseFloat(amount.replace(",", "")));
        paymentDetails.put(MOLPayActivity.mp_bill_name, user.getFirstName() + " " + user.getLastName());
        JLogUtils.i("Pconstants", "PConstants.MP_BILL_NAME：" + user.getFirstName() + " " +
                user.getLastName());
        paymentDetails.put(MOLPayActivity.mp_bill_email, user.getEmail());
        JLogUtils.i("Pconstants", "PConstants.MP_BILL_EMAIL：" + user.getEmail());
        paymentDetails.put(MOLPayActivity.mp_bill_mobile, phone);
        JLogUtils.i("Pconstants", "PConstants.MP_BILL_MOBILE：" + phone);
        paymentDetails.put(MOLPayActivity.mp_bill_description, productName + ", Shipping Fee " + shippingFee);
        JLogUtils.i("Pconstants", "PConstants.MP_BILL_DESCRIPTION：" + productName + ", Shipping Fee " + shippingFee);
        paymentDetails.put(MOLPayActivity.mp_currency, "MYR");
        JLogUtils.i("Pconstants", "PConstants.MP_CURRENCY：MYR");
        paymentDetails.put(MOLPayActivity.mp_channel, JDataUtils.isEmpty(channel) ? "multi" : channel);
        JLogUtils.i("Pconstants", "PConstants.MP_CHANNEL：" + (JDataUtils.isEmpty(channel) ? "multi" :
                channel));
//        paymentDetails.put(MOLPayActivity.MP_IS_ESCROW, false);
//        JLogUtils.i("Pconstants", "PConstants.MP_IS_ESCROW：false");
//        bundle.putBoolean(PConstants.MP_DEBUG_MODE, false);
        JLogUtils.i("Pconstants", "PConstants.MP_DEBUG_MODE：false");
        paymentDetails.put(MOLPayActivity.mp_channel_editing, true);
        JLogUtils.i("Pconstants", "PConstants.MP_EDITING_ENABLED：true");
        paymentDetails.put(MOLPayActivity.mp_editing_enabled, false);
        JLogUtils.i("Pconstants", "PConstants.MP_SECURE_MODE_ENABLED：false");
        paymentDetails.put(MOLPayActivity.mp_verification_key, method.getVERIFYKEY());
//        intent.putExtras(bundle);

        Intent intent = new Intent(CheckoutActivity.this, MOLPayActivity.class);
        intent.putExtra(MOLPayActivity.MOLPayPaymentDetails, paymentDetails);
        startActivityForResult(intent, MOLPayActivity.MOLPayXDK);
    }


//    /**
//     * GO TO Molpay page
//     * @param orderId
//     * @param amount
//
//     * @param phone
//     * @param channel
//     */
//    private void goToMolpayPage(String orderId, String amount, String shippingFee, String phone, String
//            channel, String productName) {
//        order_id = orderId;
//        GOUserEntity user = GemfiveApplication.getAppConfiguration().getUserInfo(CheckoutActivity.this);
//        Intent intent = new Intent(CheckoutActivity.this, MOLPayActivity.class);
//        Bundle bundle = new Bundle();
//        JLocalMethod  method=new JLocalMethod();
////        JLogUtils.i("Checkout","==================="+method.getMERCHANTID());
////        JLogUtils.i("Checkout", "===================" + method.getAPPNAME());
////        JLogUtils.i("Checkout", "===================" + method.getVERIFYKEY());
////        JLogUtils.i("Checkout", "===================" + method.getUSERNAME());
////        JLogUtils.i("Checkout", "===================" + method.getPASSWORD());
//        bundle.putString(PConstants.MP_MERCHANT_ID, method.getMERCHANTID());
//        JLogUtils.i("Pconstants", "PConstants.MP_MERCHANT_ID：" + method.getMERCHANTID());
//        bundle.putString(PConstants.MP_APP_NAME, method.getAPPNAME());
//        JLogUtils.i("Pconstants", "PConstants.MP_APP_NAME：" + method.getAPPNAME());
//        bundle.putString(PConstants.MP_USERNAME, method.getUSERNAME());
//        JLogUtils.i("Pconstants", "PConstants.MP_USERNAME：" + method.getUSERNAME());
//        bundle.putString(PConstants.MP_PASSWORD, method.getPASSWORD());
//        JLogUtils.i("Pconstants", "PConstants.MP_PASSWORD：" + method.getPASSWORD());
//        bundle.putString(PConstants.MP_ORDER_ID, orderId);
//        JLogUtils.i("Pconstants", "PConstants.MP_ORDER_ID：" + orderId);
//        bundle.putString(PConstants.MP_COUNTRY, "MY");
//        JLogUtils.i("Pconstants", "PConstants.MP_COUNTRY：MY");
//        if(!TextUtils.isEmpty(amount)) {
//            bundle.putString(PConstants.MP_AMOUNT, amount.replace(",", ""));
//        }
//        JLogUtils.i("Pconstants", "PConstants.MP_AMOUNT：" + Float.parseFloat(amount.replace(",", "")));
//        bundle.putString(PConstants.MP_BILL_NAME, user.getFirstName() + " " + user.getLastName());
//        JLogUtils.i("Pconstants", "PConstants.MP_BILL_NAME：" + user.getFirstName() + " " +
//                user.getLastName());
//        bundle.putString(PConstants.MP_BILL_EMAIL, user.getEmail());
//        JLogUtils.i("Pconstants", "PConstants.MP_BILL_EMAIL：" + user.getEmail());
//        bundle.putString(PConstants.MP_BILL_MOBILE, phone);
//        JLogUtils.i("Pconstants", "PConstants.MP_BILL_MOBILE：" + phone);
//        bundle.putString(PConstants.MP_BILL_DESCRIPTION, productName + ", Shipping Fee "+shippingFee);
//        JLogUtils.i("Pconstants", "PConstants.MP_BILL_DESCRIPTION：" + productName + ", Shipping Fee "+shippingFee);
//        bundle.putString(PConstants.MP_CURRENCY, "MYR");
//        JLogUtils.i("Pconstants", "PConstants.MP_CURRENCY：MYR");
//        bundle.putString(PConstants.MP_CHANNEL, JDataUtils.isEmpty(channel) ? "multi" : channel);
//        JLogUtils.i("Pconstants", "PConstants.MP_CHANNEL：" + (JDataUtils.isEmpty(channel) ? "multi" :
//                channel));
//        bundle.putBoolean(PConstants.MP_IS_ESCROW, false);
//        JLogUtils.i("Pconstants", "PConstants.MP_IS_ESCROW：false");
////        bundle.putBoolean(PConstants.MP_DEBUG_MODE, false);
//        JLogUtils.i("Pconstants", "PConstants.MP_DEBUG_MODE：false");
//        bundle.putBoolean(PConstants.MP_EDITING_ENABLED, true);
//        JLogUtils.i("Pconstants", "PConstants.MP_EDITING_ENABLED：true");
//        bundle.putBoolean(PConstants.MP_SECURE_MODE_ENABLED, false);
//        JLogUtils.i("Pconstants", "PConstants.MP_SECURE_MODE_ENABLED：false");
//        bundle.putString(PConstants.MP_VERIFICATION_KEY, method.getVERIFYKEY());
////        JLogUtils.i("Pconstants", "PConstants.MP_VERIFICATION_KEY:" + method.getVERIFYKEY());
//
////        String secureStr = String.valueOf(amount.replace(",", "")) + method.getMERCHANTID() + orderId +
////                method.getVERIFYKEY()+"B3";
////        bundle.putString(PConstants.MP_VERIFICATION_KEY, vcode);
////        JLogUtils.i("Pconstants", "PConstants.MP_VERIFICATION_KEY:" + secureStr + "----md5:" + get32MD5
////                (secureStr)); get32MD5(secureStr);
//
//
//
//
////        bundle.putBoolean(PConstants.MP_CUSTOM_CC,true);
////        bundle.putString("DefaultCountry", "Malaysia");
//
//        //Merchant Info
////        bundle.putString("MerchantId", method.getMERCHANTID());
////        bundle.putString("AppName", method.getAPPNAME());
////        bundle.putString("VerifyKey", method.getVERIFYKEY());
////        bundle.putString("Username", method.getUSERNAME());
////        bundle.putString("Password", method.getPASSWORD());
////
//
//
////        String secureStr = String.valueOf(amount.replace(",", "")) + method.getMERCHANTID() + orderId +
////        method.getVERIFYKEY() + "B3";
////        try {
////
////            bundle.putString("VerifyKey", getMD5(secureStr));
////        } catch (NoSuchAlgorithmException e) {
////            e.printStackTrace();
////
////        }          //bundle.putBoolean("IsSecure", true);
////
//
//
//
//
////        //Buyer Info
////        bundle.putString("OrderId", orderId);
////        bundle.putString("BillName", user.getFirstName() + " " + user.getLastName());
////        bundle.putString("BillEmail", user.getEmail());
////        bundle.putFloat("Amount", Float.parseFloat(amount.replace(",", "")));
////
////        System.out.println("channel========*********=========" + channel);
////
////        bundle.putString("BillDesc", productName + ", Shipping Fee " + shippingFee);//"Purchase of 5 pcs
////        of survivor kits");
////        bundle.putString("BillMobile", phone);
////        //bundle.putString("Channel", "multi");
////        bundle.putString("Channel", JDataUtils.isEmpty(channel) ? "multi" : channel);
////        bundle.putString("Currency", "MYR");
////        bundle.putString("Country", "MY");
////        bundle.putBoolean("is_escrow", false);//make this true to enable to enable escrow
////        bundle.putBoolean("debug", false);
////        bundle.putBoolean("editable", true);
////
////        bundle.putBoolean("ShowCCDetails", true);
////        bundle.putString("DefaultCountry", "Malaysia");
////        bundle.putBoolean("ShowTimer", true);
////        bundle.putLong("Duration", 60 * 8000);//sample 1 minute
//
//        intent.putExtras(bundle);
//        startActivityForResult(intent, REQUEST_CODE_MOLPAY);
//    }

//    /**
//     * GO TO Molpay page
//     * @param orderId
//     * @param amount
//
//     * @param phone
//     * @param channel
//     */
//    private void goToMolpayPage(String orderId, String amount, String shippingFee, String phone, String channel, String productName) {
//        order_id = orderId;
//        GOUserEntity user = GemfiveApplication.getAppConfiguration().getUserInfo(CheckoutActivity.this);
//        Intent intent = new Intent(CheckoutActivity.this, MOLPayActivity.class);
//        Bundle bundle = new Bundle();
//        JLocalMethod  method=new JLocalMethod();
////        JLogUtils.i("ray","==================="+method.getMERCHANTID());
////        JLogUtils.i("ray", "===================" + method.getAPPNAME());
////        JLogUtils.i("ray", "===================" + method.getVERIFYKEY());
////        JLogUtils.i("ray", "===================" + method.getUSERNAME());
////        JLogUtils.i("ray", "===================" + method.getPASSWORD());
//        //Merchant Info
//        bundle.putString("MerchantId", method.getMERCHANTID());
//        bundle.putString("AppName", method.getAPPNAME());
//        bundle.putString("VerifyKey", method.getVERIFYKEY());
//        bundle.putString("Username", method.getUSERNAME());
//        bundle.putString("Password", method.getPASSWORD());
//        //bundle.putBoolean("IsSecure", true);
//        String secureStr = String.valueOf(amount.replace(",", "")) + method.getMERCHANTID() + orderId + method.getVERIFYKEY() + "B3";
//        //bundle.putString("VerifyKey", string2MD5(secureStr));
//        //Buyer Info
//        bundle.putString("OrderId", orderId);
//        bundle.putString("BillName", user.getFirstName() + " " + user.getLastName());
//        bundle.putString("BillEmail", user.getEmail());
//        bundle.putFloat("Amount", Float.parseFloat(amount.replace(",", "")));
//        System.out.println("channel========*********=========" + channel);
//        bundle.putString("BillDesc", productName + ", Shipping Fee " + shippingFee);//"Purchase of 5 pcs of survivor kits");
//        bundle.putString("BillMobile", phone);
//        //bundle.putString("Channel", "multi");
//        bundle.putString("Channel", JDataUtils.isEmpty(channel) ? "multi" : channel);
//        bundle.putString("Currency", "MYR");
//        bundle.putString("Country", "MY");
//        bundle.putBoolean("is_escrow", false);//make this true to enable to enable escrow
//        bundle.putBoolean("debug", false);
//        bundle.putBoolean("editable", true);
//        bundle.putBoolean("ShowCCDetails", true);
//        bundle.putString("DefaultCountry", "Malaysia");
//        bundle.putBoolean("ShowTimer", true);
//        bundle.putLong("Duration", 60 * 8000);//sample 1 minute
//        intent.putExtras(bundle);
//        startActivityForResult(intent, REQUEST_CODE_MOLPAY);
//    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (REQUESTCODE_LOGIN == requestCode) {
            if (GemfiveApplication.getAppConfiguration().isSignIn(CheckoutActivity.this)) {
                JLogUtils.i("checkout-continue-option---->", "login back");
                startNextActivity(null, ShoppingCartActivity1.class, true);
            }
            return;
        }
        String transaction_id = "";
        String transaction_status = "";
        String orderId = "";
        String channel = "";
        String currency = "";
        String payDate = "";
        String appCode = "";
        if (requestCode == MOLPayActivity.MOLPayXDK) {
            JLogUtils.i(TAG, "RESULT_OK : " + resultCode);
            mCheckoutPaymentDialog = new CheckoutPaymentDialog(CheckoutActivity.this, R.style.loading_dialog, getResources().getString(R.string.payment_molpay_hint)).showDialog();
            if (resultCode == RESULT_OK) {
                if (data != null) {
                    String result = data.getStringExtra(MOLPayActivity.MOLPayTransactionResult);
                    MolpayResponseBean bean = JJsonUtils.parseJsonObj(result, MolpayResponseBean.class);
                    if(bean!=null) {
                        amount = bean.getAmount();
                        transaction_status = bean.getStatus_code();
                        orderId = bean.getOrder_id();
                        channel = bean.getChannel();
                        payDate = bean.getPaydate();
                        appCode = bean.getApp_code();
                        currency = "MYR";
                        transaction_id = bean.getTxn_ID();
                    }
                }
            }

            if (TextUtils.isEmpty(orderId)) {
//                mDialog=JViewUtils.showProgressDialog(CheckoutActivity.this);
                mShoppingCarDao.sendRecoverOrder(GemfiveApplication.getAppConfiguration().getUserInfo(this).getSessionKey(), order_id, "");
                return;
            }
//            if (TextUtils.isEmpty(orderId)) {
//                orderId = order_id;
//            }
            mCheckoutDao.changeOrderStatus(GemfiveApplication.getAppConfiguration().getUserInfo(this).getSessionKey(),
                    orderId, transaction_status, channel, transaction_id, payDate, amount, currency, appCode);
        }


    }

//        if (requestCode == MOLPayActivity.MOLPayXDK) {
//            if (resultCode == RESULT_OK) {
//                if (data != null) {
//                    String result = data.getStringExtra(MOLPayActivity.MOLPayTransactionResult);
//                    MolpayResponseBean bean = JJsonUtils.parseJsonObj(result, MolpayResponseBean.class);
//                    amount = bean.getAmount();
//                    transaction_status = bean.getStatus_code();
//                    orderId = bean.getOrder_id();
//                    channel = bean.getChannel();
//                    payDate = bean.getPaydate();
//                    appCode = bean.getApp_code();
//                    currency = "MYR";
//                    transaction_id = bean.getTxn_ID();
//
//                    JViewUtils.showMaterialDialog(CheckoutActivity.this, "", result + "a", new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//
//                        }
//                    });
//
//                }
//            } else {
//                JViewUtils.showMaterialDialog(CheckoutActivity.this, "", "RESULT_OK:"+RESULT_OK, new View.OnClickListener() {
//                    @Override
//                    public void onClick(View v) {
//
//                    }
//                });
////                if (TextUtils.isEmpty(orderId)) {
////                    mShoppingCarDao.sendRecoverOrder(GemfiveApplication.getAppConfiguration().getUserInfo(this).getSessionKey(), order_id, "");
////                    return;
////                }
//            }
//        }else{
//            JViewUtils.showMaterialDialog(CheckoutActivity.this, "", "requestCode:"+requestCode, new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//
//                }
//            });
//        }


//
//    /**
//     * accept molpay result and deal next
//     *
//     * @param requestCode
//     * @param resultCode
//     * @param data
//     */
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        System.out.println("******************molpay***back*****requestCode***************"+requestCode);
//        if (REQUESTCODE_LOGIN == requestCode) {
//            /**
//             * If sesion-expiration occured during payment process,
//             * turn to login page ,
//             * then go back to shoppingcart page
//             */
//            if (GemfiveApplication.getAppConfiguration().isSignIn(CheckoutActivity.this)) {
//                JLogUtils.i("checkout-continue-option---->", "login back");
//                startNextActivity(null, ShoppingCartActivity1.class, true);
//            }
//            return;
//        }
//        //progressBarLoading.setVisibility(View.VISIBLE);
//        mDialog = JViewUtils.showProgressDialog(CheckoutActivity.this);
//        scrollViewBody.setVisibility(View.GONE);
//        ll_btn.setVisibility(View.GONE);
//        String transaction_id = "";
//        String transaction_status = "";
//        String orderId = "";
//        String channel = "";
//        String currency = "";
//        String payDate = "";
//        String appCode = "";
////        boolean isSuccess=false;
//        if (requestCode == REQUEST_CODE_MOLPAY) {
//            if(resultCode == RESULT_OK) {
//             if(data!=null){
//                    amount = data.getStringExtra(PConstants.MP_AMOUNT);
////                  appCode=bundle.getString(PConstants.);
//                    transaction_id = data.getStringExtra(PConstants.MP_TRANSACTION_ID);
//                    transaction_status = data.getStringExtra(PConstants.MP_STATUS_CODE);
//                    orderId = data.getStringExtra(PConstants.MP_ORDER_ID);
//                    currency = data.getStringExtra(PConstants.MP_CURRENCY);
////                  payDate=bundle.getString(PConstants.MP_);
//                    channel = data.getStringExtra(PConstants.MP_CHANNEL);
//                    String bill_desc = data.getStringExtra(PConstants.MP_BILL_DESCRIPTION);
//                    shippingFee = bill_desc.split("Shipping Fee")[1];
//                    if (!JDataUtils.isEmpty(shippingFee)) {
//                        shippingFee = shippingFee.trim();
//                    }
////                    isSuccess = true;
////                amount = bundle.getString(MerchantInfo.PAY_AMOUNT);
////                appCode = bundle.getString(MerchantInfo.APP_CODE);
////                transaction_id = bundle.getString(MerchantInfo.TXN_ID);
////                transaction_status = bundle.getString(MerchantInfo.STATUS_CODE);
////                String error_desc = bundle.getString(MerchantInfo.ERR_DESC);
////                orderId = bundle.getString(MerchantInfo.ORDER_ID);
////                currency = bundle.getString(MerchantInfo.CURRENCY);
////                payDate = bundle.getString(MerchantInfo.PAYDATE);
////                String bill_name = bundle.getString(MerchantInfo.BILL_NAME);
////                String bill_email = bundle.getString(MerchantInfo.BILL_EMAIL);
////                String bill_mobile = bundle.getString(MerchantInfo.BILL_MOBILE);
////                String bill_desc = bundle.getString(MerchantInfo.BILL_DESC);
////                shippingFee = bill_desc.split("Shipping Fee")[1];
////                if (!JDataUtils.isEmpty(shippingFee)) {
////                    shippingFee = shippingFee.trim();
////                }
////                channel = bundle.getString(MerchantInfo.CHANNEL);
////                String result = "**********the transaction_status is " + transaction_status + "\nthe name is " + bill_name + "\nthe mobile is " + bill_mobile + "\nthe email is " + bill_email + "\nthe description is " + bill_desc + ", shippingFee = " + shippingFee + "\nthe amount is " + amount + "\nthe transaction id is " + transaction_id;
//                }
//            }else{
//                if (TextUtils.isEmpty(orderId)) {
//                    mShoppingCarDao.sendRecoverOrder(GemfiveApplication.getAppConfiguration().getUserInfo(this).getSessionKey(), order_id, "");
//                    return;
//                }
//            }
//        }
//        if(TextUtils.isEmpty(orderId)){
//            orderId=order_id;
//        }
////        String str="resultCode:"+resultCode+"\n"+"isSuccess:"+isSuccess+"\n"+"transaction_id:"+transaction_id+"  \n"+"transaction_status:"+transaction_status+"\n"+"orderId:"+orderId+" \n"+"channel:"+channel+" \n"+"currency:"+currency+" \n"+"payDate:"+payDate+" \n"+"appCode:"+appCode;
////        JViewUtils.showMaterialDialog(CheckoutActivity.this, "", str, new View.OnClickListener() {
////            @Override
////            public void onClick(View view) {
////
////            }
////        });
//        mCheckoutDao.changeOrderStatus(GemfiveApplication.getAppConfiguration().getUserInfo(this).getSessionKey(),
//                orderId,transaction_status,channel,transaction_id,payDate,amount,currency,appCode);
//
//
//
//        /**
//         * Call WebService
//         */
////        SVRParameters parameters = new SVRParameters();
////        parameters.put("session_key",  GemfiveApplication.getAppConfiguration().getUserInfo(this).getSessionKey());
////        parameters.put("orderid", orderId);
////        parameters.put("method", "molpay");
////        parameters.put("status", transaction_status);
////        parameters.put("channel", channel);
////        parameters.put("tranID", transaction_id);
////        parameters.put("paydate", payDate);
////        parameters.put("amount", amount);
////        parameters.put("currency", currency);
////        parameters.put("appCode", appCode);
////        SVRCheckoutMolpayReturnChangeOrderStatus changeOrderStatusHandler = new SVRCheckoutMolpayReturnChangeOrderStatus(CheckoutActivity.this, parameters);
////        JLogUtils.i("russell->amount", amount);
////        JLogUtils.i("russell->shippingFee", shippingFee);
////        changeOrderStatusHandler.loadDatasFromServer(new SVRCallback() {
////            @Override
////            public void onSuccess(int resultCode, SVRReturnEntity result) {
////                if (mDialog != null) {
////                    mDialog.cancel();
////                }
////                Bundle bundle_success = new Bundle();
////                bundle_success.putString("payment_status", "1");
////                bundle_success.putString("lastrealorderid", order_id);
////                bundle_success.putString("grand_total", amount);
////                bundle_success.putString("shipping_fee", shippingFee);
////                bundle_success.putSerializable("paymentSaveReturnEntity", paymentSaveReturnEntity);
////                bundle_success.putInt("fromType", fromType);
////                bundle_success.putInt("paymentMethod",CheckoutPaymentRedirectActivity.PAYMENT_ONLINE);
////                if(mDiscountBean!=null){
////                    bundle_success.putSerializable("discountBean",mDiscountBean);
////                }
////                startNextActivity(bundle_success, CheckoutPaymentRedirectActivity.class, true);
////                //progressBarLoading.setVisibility(View.GONE);
////            }
////            @Override
////            public void onFailure(int resultCode, String errorMsg) {
////                //progressBarLoading.setVisibility(View.GONE);
////                if (mDialog != null) {
////                    mDialog.cancel();
////                }
////                if (!JDataUtils.errorMsgHandler(CheckoutActivity.this, errorMsg)) {
////                    if ((!JDataUtils.isEmpty(errorMsg)) && (errorMsg.contains(SESSION_EXPIRED))) {
////                        Intent intent = new Intent();
////                        intent.setClass(CheckoutActivity.this, LoginRegisterActivity.class);
////                        Bundle bundle = new Bundle();
////                        bundle.putBoolean("expire", true);
////                        intent.putExtras(bundle);
////                        startActivityForResult(intent, REQUESTCODE_LOGIN);
////                    } else {
////                        Bundle bundle_failuer = new Bundle();
////                        bundle_failuer.putString("payment_status", "0");
////                        bundle_failuer.putString("errorMsg", errorMsg);
////                        bundle_failuer.putString("orderNumber", order_id);
////                        if (mDiscountBean != null) {
////                            bundle_failuer.putSerializable("discountBean", mDiscountBean);
////                        }
////                        bundle_failuer.putInt("fromType",fromType);
////                        startNextActivity(bundle_failuer, CheckoutPaymentStatusActivity.class, true);
////                    }
////                }
////            }
////        });
//    }


    public void retryOrder(String orderId) {
        mShoppingCarDao.sendRecoverOrder(GemfiveApplication.getAppConfiguration().getUserInfo(CheckoutActivity.this).getSessionKey(), orderId, "");
    }

    public void startShoppingActivity() {
        Intent intent = new Intent(CheckoutActivity.this, ShoppingCartActivity1.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        overridePendingTransition(R.anim.enter_lefttoright, R.anim.exit_lefttoright);
        finish();
    }

    public void saveShoppingCartCount(int num) {
        GOUserEntity userEntity = GemfiveApplication.getAppConfiguration().getUserInfo(CheckoutActivity.this);
        userEntity.setCartItemCount(num);
        GemfiveApplication.getAppConfiguration().updateDate(CheckoutActivity.this, userEntity);
    }


    @Override
    protected void onStart() {
        super.onStart();
//        GaTrackHelper.getInstance().googleAnalyticsReportActivity(this, true);
//        GaTrackHelper.getInstance().googleAnalyticsStartCheckout(CheckoutActivity.this, productIds, "checkout", 1);

    }

    @Override
    protected void onDestroy() {
        if (mAccountDao != null) {
            mAccountDao.cancelHttpByTag(TAG);
        }
        if (mCheckoutDao != null) {
            mCheckoutDao.cancelHttpByTag(TAG);
        }
        if (mHandler != null) {
            mHandler.removeCallbacksAndMessages(null);
        }
        closeCheckoutPaymentDialog();
        super.onDestroy();
//        GemfiveApplication.getRefWatcher(this).watch(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
//        GaTrackHelper.getInstance().googleAnalyticsReportActivity(this, false);
    }

}