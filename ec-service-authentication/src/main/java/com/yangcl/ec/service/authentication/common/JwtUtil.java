package com.yangcl.ec.service.authentication.common;

import com.yangcl.ec.common.entity.common.LoginAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class JwtUtil {
   private static final String CLAIM_KEY_USER_ACCOUNT="sub";
   private static final String CLAIM_KEY_CREATED="created";

   @Value("${jwt.secret}")
   private String secret;

   @Value("${jwt.expiration}")
   private Long expiration;

   /**
    * 生成Token
    * @return
    */
   public String createdToken(Map<String,Object> claims){
      return Jwts.builder()
              .setClaims(claims)
              .setExpiration(new Date(System.currentTimeMillis()+this.expiration*1000))
              .signWith(SignatureAlgorithm.HS256,this.secret)
              .compact();
   }

   /**
    * 生成Token
    * @param loginAccount
    * @return
    */
   public String createdToken(LoginAccount loginAccount){
      Map<String,Object> claims=new HashMap<String,Object>();
      claims.put("accountId",loginAccount.getAccountId());
      claims.put("accountName",loginAccount.getAccountName());
      claims.put("username",loginAccount.getUsername());
      claims.put("password",loginAccount.getPassword());
      claims.put("otherName",loginAccount.getOtherName());
      claims.put("permissions",loginAccount.getPermissions());
      return this.createdToken(claims);
   }

   /**
    * 刷新token
    * @param token
    * @return
    */
   public String refreshToken(String token){
      String refreshedToken;
      try{
         final Claims claims=getClaimsFromToken(token);
         claims.put(CLAIM_KEY_CREATED,new Date());
         refreshedToken=createdToken(claims);
      }catch (Exception err){
         refreshedToken=null;
      }
      return refreshedToken;
   }

   /**
    * 验证token
    * @param token
    * @return
    */
   public Boolean validateToken(String token){
      Boolean result=(!isTokenExpired(token));
      return result;
   }

   /**
    * 验证token
    * @param token
    * @param loginAccount
    * @return
    */
   public Boolean validateToken(String token,LoginAccount loginAccount){
      Boolean result=(
              loginAccount.getAccountId().equals(this.getValueFromToken(token,"accountId"))
              && loginAccount.getUsername().equals(this.getValueFromToken(token,"username"))
              && !this.isTokenExpired(token));
      return result;
   }

   /**
    * 从token中获取用户
    * @param token
    * @return
    */
   public String getAccountFromToken(String token){
      String account;
      try{
         final Claims claims=getClaimsFromToken(token);
         account=claims.getSubject();
      }catch (Exception err){
         account=null;
      }
      return account;
   }

   /**
    * 从token中获取登录用户
    * @param token
    * @return
    */
   public LoginAccount getLoginAccountFromToken(String token){
      LoginAccount loginAccount;
      try{
         final Claims claims=getClaimsFromToken(token);
         loginAccount=new LoginAccount(
                 claims.get("accountId").toString(),
                 claims.get("accountName").toString(),
                 claims.get("username").toString(),
                 claims.get("password").toString(),
                 claims.get("otherName").toString(),
                 (List<String>)claims.get("permissions")
         );
      }catch (Exception err){
         loginAccount=null;
      }
      return loginAccount;
   }

   /**
    * 从token中获取自定义信息
    * @param token
    * @param key
    * @return
    */
   public Object getValueFromToken(String token,String key){
      Object result;
      try{
         final Claims claims=getClaimsFromToken(token);
         result=claims.get(key);
      }catch (Exception err){
         result=null;
      }
      return result;
   }

   /**
    * 从token中获取创建时间
    * @param token
    * @return
    */
   public Date getCreatedDateFromToken(String token){
      Date created;
      try{
         final Claims claims=getClaimsFromToken(token);
         created=new Date((Long)claims.get(CLAIM_KEY_CREATED));
      }catch (Exception err){
         created=null;
      }
      return created;
   }

   /**
    * 获取token过期时间
    * @param token
    * @return
    */
   public Date getExpirationDateFromToken(String token){
      Date expiration;
      try{
         final Claims claims=getClaimsFromToken(token);
         expiration=claims.getExpiration();
      }catch (Exception err){
         expiration=null;
      }
      return expiration;
   }

   /**
    * 从token中获取claims
    * @param token
    * @return
    */
   private Claims getClaimsFromToken(String token){
      Claims claims;
      try{
         claims=Jwts.parser()
                 .setSigningKey(secret)
                 .parseClaimsJws(token)
                 .getBody();
      }catch (Exception err){
         claims=null;
      }
      return claims;
   }

   /**
    * 判断token是否过期
    * @param token
    * @return
    */
   private Boolean isTokenExpired(String token){
      final Date expiration=getExpirationDateFromToken(token);
      if(expiration==null)
         return true;
      return expiration.before(new Date());
   }
}
