package com.tinglans.backend.thirdparty.amap;

import com.tinglans.backend.config.AmapConfig;
import com.tinglans.backend.thirdparty.amap.dto.AmapPoi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * 高德地图客户端测试
 */
@SpringBootTest
class AmapClientTest {
    
    @Autowired
    private AmapConfig amapConfig;
    
    private AmapClient amapClient;
    
    @BeforeEach
    void setUp() {
        WebClient.Builder webClientBuilder = WebClient.builder();
        amapClient = new AmapClient(amapConfig, webClientBuilder);
    }
    
    @Test
    void testSearchXiamenLongtouRoad() {
        String destination = "厦门";
        String locationName = "龙头路美食街";
        
        System.out.println("\n========== 高德地图 POI 搜索测试 ==========");
        System.out.println("目的地: " + destination);
        System.out.println("位置名: " + locationName);
        System.out.println();
        
        // 调用带 region 参数的搜索
        AmapPoi poi = amapClient.searchLocation(locationName, destination);
        
        if (poi != null) {
            System.out.println("✅ 搜索成功！POI 信息如下：");
            System.out.println("  POI ID: " + poi.getId());
            System.out.println("  名称: " + poi.getName());
            System.out.println("  类型: " + poi.getType());
            System.out.println("  分类编码: " + poi.getTypecode());
            System.out.println("  地址: " + poi.getAddress());
            System.out.println("  坐标 (location): " + poi.getLocation());
            System.out.println("  纬度: " + poi.getLat());
            System.out.println("  经度: " + poi.getLng());
            System.out.println("  省份: " + poi.getPname());
            System.out.println("  城市: " + poi.getCityname());
            System.out.println("  区县: " + poi.getAdname());
            System.out.println("  城市编码: " + poi.getCitycode());
            System.out.println("  区编码: " + poi.getAdcode());
            System.out.println("  电话: " + poi.getTel());
        } else {
            System.out.println("❌ 未找到匹配的 POI");
        }
        System.out.println("==========================================\n");
    }
}
