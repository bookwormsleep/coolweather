package com.coolweather.android;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.coolweather.android.db.City;
import com.coolweather.android.db.County;
import com.coolweather.android.db.Province;
import com.coolweather.android.util.HttpUtil;
import com.coolweather.android.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


/**
 * Created by jackie on 2017/9/9.
 */
public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0 ;
    public static final int LEVEL_CITY = 1 ;
    public static final int LEVEL_COUNTY = 2 ;
    private ProgressDialog progressDialog ;
    private TextView titleText ;
    private Button backButton ;
    private ListView listView ;
    private ArrayAdapter<String> adapter ;
    private List<String> dataList = new ArrayList<>() ;
    /**
     * 省例表
     */
    private List<Province> provinceList ;
    /**
     * 市列表
     */
    private List<City> cityList ;
    private List<County> countyList ;
    /**
     * 选中的省份
     */
    private Province selectedProvince ;
    /**
     * 选中的城市
     */
    private City selectedCity;
    /**
     * 当前选中的级别
     */
    private int currentLevel ;

    /**
     * 获取控件实例
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_ares, container, false) ;
        titleText = (TextView) view.findViewById(R.id.title_text);   //获取控件实例
        backButton = (Button) view.findViewById(R.id.back_button);
        listView = (ListView) view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList) ;//初始化ArrayAdapter
        listView.setAdapter(adapter);       //将adapter设置为listView的适配器
        return view;
    }

    /**
     * 在onActivityCreated()方法中给ListView 和 Button设置点击事件，到这里初始化工作完成
     * @param savedInstanceState
     */
    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            /**
             * 点击某个省进入ListView的onItemClick()方法中，根据currentLevel来判断调用queryCities()，queryCounties()方法。
             * @param parent
             * @param view
             * @param position
             * @param id
             */
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {           //根据当前级别来判断调用queryCities()方法
                    selectedProvince = provinceList.get(position) ;
                    queryCities() ;
                } else if (currentLevel == LEVEL_CITY) {         //根据当前级别来判断调用queryCounties()方法
                    selectedCity = cityList.get(position) ;
                    queryCounties();
                    /**
                     * 加入if判断，如果当前级别是LEVEL_COUNTY，就启动WeatherActivity，并把当前选中县的天气id传递过去。
                     */
                }/**else if (currentLevel == LEVEL_COUNTY) {
                    String weatherId = countyList.get(position).getWeatherId();
                    Intent intent = new Intent(getActivity(),WeatherActivity.class);
                    intent.putExtra("weather_id",weatherId);
                    startActivity(intent);
                    getActivity().finish();
                }*/
            }
        });
        /**
         * 对当前ListViewr列表级别进行判断，县级返回市级，市级返回省级，省级返回按钮会自动隐藏。
         */
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY) {
                    queryCities();
                }else if (currentLevel == LEVEL_CITY) {
                    queryProvinces() ;
                }
            }
        });
        /**
         * 在onActivityCreated()就去的最后，调用queryProvinces()方法，从这里开始加载省级数据。
         */
        queryProvinces() ;
    }

    /**
     * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询
     */
    private void queryProvinces() {
        /**
         * queryProvinces()方法中首先将头布局设置成中国
         */
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);  //将返回按钮隐藏起来，省级列表不能返回
        provinceList = DataSupport.findAll(Province.class) ;//读取省级数据
        if (provinceList.size() > 0 ) {
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName()) ;
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE ;
        } else {
            String address = "http://guolin.tech/api/china" ;
            queryFromServer(address, "province") ;
        }
    }

    /**
     * 查询选中省内的所有市，优先从数据库查询，如果没有查询到再去服务器上查询
     */
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);     //显示返回按钮
        cityList = DataSupport.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0 ) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName()) ;
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY ;
        }else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china" + provinceCode ;
            queryFromServer(address, "city");
        }
    }

    /**
     * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询
     */
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?",String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0 ){
            dataList.clear();
            for (County county : countyList){
                dataList.add(county.getCountyName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTY ;
        }else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode() ;
            String address = "http://guolin.tech/api/china/16/116" + provinceCode + "/" + cityCode ;
            queryFromServer(address, "county");
        }
    }

    /**http://v.juhe.cn/weather/citys?key=66545fdc46b6deca436473cdd15f0704
     * 根据传入的地址和类型从服务器上查询省市县数据
     * @param address
     * @param type
     */

    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {//1.向服务器发送请求，

            @Override
            public void onResponse(Call call, Response response) throws IOException { //2.响应数据会回调到onResponse()方法中
                String responseText = response.body().string();
                boolean result = false ;
                if ("province".equals(type)){
                    result = Utility.handleProvinceResponse(responseText); //3.解析和处理服务器返回的数据，并存储到数据库中
                }else if ("city".equals(type)){
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());
                }else if ("county".equals(type)){
                    result = Utility.handleCountyResponse(responseText,selectedCity.getId());
                }
                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog() ;
                            if ("province".equals(type)){
                                queryProvinces();
                            }else if ("city".equals(type)){
                                queryCities();
                            }else if ("county".equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }
            }

            /**
             * 通过runOnUiThread()方法回到主线程处理逻辑
             * @param call
             * @param e
             */
            @Override
            public void onFailure(Call call, IOException e) {
                //从子线程切换到主线程
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity()) ;
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }
}
