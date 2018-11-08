package cn.csdb.portal.repository;

import cn.csdb.portal.model.ResCatalog_Mongo;
import cn.csdb.portal.model.User;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.Set;

/**
 * Created by shiba on 2018/11/6.
 */
@Repository
public class CheckUserDao {
    @Resource
    private MongoTemplate mongoTemplate;
    /**
     *  通过用户名查找用户
     *  @param userName
     *  @return User
     */
    public User getByUserName(String userName){
        return mongoTemplate.find(new Query(Criteria.where("userName").is(userName)),User.class).get(0);
    }

    /**
     *  通过用户名查找该用户所有的角色并保存在Set集合中
     *  @param username
     *  @return Set<String>
     */
    /*public Set<String> getRoles(String username){

    }*/

    /**
     *  通过用户名查找该用户所有的权限并保存在Set集合中
     *  @param username
     *  @return Set<String>
     */
    /*public Set<String> getPermissions(String username){

    }*/

}