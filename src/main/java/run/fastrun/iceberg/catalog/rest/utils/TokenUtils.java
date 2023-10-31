package run.fastrun.iceberg.catalog.rest.utils;


import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.util.HashMap;

/**
 * @Auther: dupeng
 * @Date: 2023/10/29/13:03
 * @Description:
 */
public class TokenUtils {
    private static final String ENV_REST_CATALOG_ACCESS_TOKEN_SECRET = "REST_CATALOG_ACCESS_TOKEN_SECRET";

    public static String getToken(){
        String tokenSecret = StringUtils.getEnv(ENV_REST_CATALOG_ACCESS_TOKEN_SECRET);
        if(tokenSecret == null) {
            throw new RuntimeException("Env. value of REST_CATALOG_ACCESS_TOKEN_SECRET is null!");
        }
        try{
            Algorithm algorithm = Algorithm.HMAC256(tokenSecret);
            HashMap<String, Object> header = new HashMap<>(2);
            header.put("Type", "Jwt");
            header.put("alg", "HS256");
            return JWT.create()
                    .withHeader(header)
                    .sign(algorithm);
        }catch (Exception e){
            e.printStackTrace();
            return null;
        }
    }
}
