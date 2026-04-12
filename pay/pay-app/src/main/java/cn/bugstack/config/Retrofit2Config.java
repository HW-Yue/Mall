package cn.bugstack.config;

import cn.bugstack.infrastructure.gateway.IGroupBuyMarketService;
import cn.bugstack.infrastructure.gateway.INorTradeService;
import cn.bugstack.infrastructure.gateway.IWeixinApiService;
import cn.bugstack.infrastructure.gateway.IProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

@Slf4j
@Configuration
public class Retrofit2Config {

    @Value("${app.config.group-buy-market.api-url}")
    private String groupBuyMarketApiUrl;

    @Bean
    public IWeixinApiService weixinApiService() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api.weixin.qq.com/")
                .addConverterFactory(JacksonConverterFactory.create()).build();

        return retrofit.create(IWeixinApiService.class);
    }

    @Bean
    public Retrofit groupBuyRetrofit() {
        return new Retrofit.Builder()
                .baseUrl(groupBuyMarketApiUrl)
                .addConverterFactory(JacksonConverterFactory.create())
                .build();
    }

    @Bean
    public IGroupBuyMarketService groupBuyMarketService(Retrofit groupBuyRetrofit) {
        return groupBuyRetrofit.create(IGroupBuyMarketService.class);
    }

    @Bean
    public IProductService productService(Retrofit groupBuyRetrofit) {
        return groupBuyRetrofit.create(IProductService.class);
    }

    @Bean
    public INorTradeService norTradeService(Retrofit groupBuyRetrofit) {
        return groupBuyRetrofit.create(INorTradeService.class);
    }

}
