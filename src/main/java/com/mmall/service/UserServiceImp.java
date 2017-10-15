package com.mmall.service;

import com.mmall.commom.Const;
import com.mmall.commom.ServerResponse;
import com.mmall.commom.TokenCache;
import com.mmall.dao.UserMapper;
import com.mmall.pojo.User;
import com.mmall.utils.MD5Util;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service("userServiceImp")
public class UserServiceImp implements IUserService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public ServerResponse<User> login(String userName, String password) {
        int result = userMapper.checkUserName(userName);
        if (result == 0) {
            return ServerResponse.createByErroMessage("用户名不存在");
        }
        password = MD5Util.MD5EncodeUtf8(password);
        User user = userMapper.selectLogin(userName, password);
        if (user == null) {
            return ServerResponse.createBySuccessMessage("密码错误");
        }
        user.setPassword(StringUtils.EMPTY);
        return ServerResponse.createBySuccess("登录成功", user);
    }

    public ServerResponse<String> register(User user) {
        ServerResponse<String> validResult = checkValid(user.getUsername(), Const.USERNAME);
        if (!validResult.isSuccess()) {
            return validResult;
        }
        validResult = checkValid(user.getUsername(), Const.EMAIL);
        if (!validResult.isSuccess()) {
            return validResult;
        }
        user.setRole(Const.Role.ROLE_CUSTOMER);
        user.setPassword(MD5Util.MD5EncodeUtf8(user.getPassword()));
        int result = userMapper.insert(user);
        if (result == 0) {
            return ServerResponse.createByErroMessage("注册失败");
        }
        return ServerResponse.createBySuccessMessage("注册成功");
    }

    @Override
    public ServerResponse<String> checkValid(String account, String type) {
        if (StringUtils.isBlank(type)) {
            return ServerResponse.createByErroMessage("参数错误");
        }
        if (Const.USERNAME.equals(account)) {
            int result = userMapper.checkUserName(account);
            if (result > 0) {
                return ServerResponse.createByErroMessage("用户名已存在");
            }
        }
        if (Const.EMAIL.equals(account)) {
            int result = userMapper.checkEmail(account);
            if (result > 0) {
                return ServerResponse.createByErroMessage("邮箱已存在");
            }
        }

        return ServerResponse.createBySuccessMessage("注册成功");
    }

    @Override
    public ServerResponse<String> selectQuestion(String userName) {
        ServerResponse result = this.checkValid(userName, Const.USERNAME);
        if (!result.isSuccess()) {
            return ServerResponse.createByErroMessage("用户不存在");
        }

        String question = userMapper.selectQuestionByUsername(userName);
        if (StringUtils.isNotBlank(question)) {
            return ServerResponse.createBySuccess(question);
        }
        return ServerResponse.createByErroMessage("无找回密码");
    }

    @Override
    public ServerResponse<String> checkAnswer(String username, String question, String answer) {
        int result = userMapper.checkAnswer(username, question, answer);
        if (result > 0) {
            String forgetToken = UUID.randomUUID().toString();
            TokenCache.setKey(TokenCache.TOKEN_PREFIX + username, forgetToken);
            return ServerResponse.createBySuccess(forgetToken);
        }

        return ServerResponse.createByErroMessage("问题的答案错误");
    }

    @Override
    public ServerResponse<String> forgetRestPassword(String userName, String newPassword, String forgetToken) {
        if (StringUtils.isBlank(forgetToken)) {
            return ServerResponse.createByErroMessage("参数错误，token需传递");
        }
        ServerResponse checkResult = this.checkValid(userName, Const.USERNAME);
        if (!checkResult.isSuccess()) {
            return ServerResponse.createByErroMessage("用户不存在");
        }
        String token = TokenCache.getKey(TokenCache.TOKEN_PREFIX + userName);
        if (StringUtils.isBlank(token)) {
            return ServerResponse.createByErroMessage("Token无效或过期");
        }
        if (!StringUtils.equals(forgetToken, token)) {
            ServerResponse.createByErroMessage("请重新获取Token");
        }

        String md5Password = MD5Util.MD5EncodeUtf8(newPassword);
        int dbResult = userMapper.updatePasswordByUsername(userName, md5Password);
        if (dbResult > 0) {
            return ServerResponse.createBySuccessMessage("重置密碼成功");
        }
        return ServerResponse.createByErroMessage("密码重置失败");
    }

    @Override
    public ServerResponse<String> resetPassword(User user, String oldPassword, String newPassword) {
        String md5Password = MD5Util.MD5EncodeUtf8(oldPassword);
        int dbResult = userMapper.checkPassword(user.getUsername(), md5Password);
        if (dbResult <= 0) {
            return ServerResponse.createByErroMessage("原始密码错误");
        }
        md5Password = MD5Util.MD5EncodeUtf8(newPassword);
        user.setPassword(md5Password);
        dbResult = userMapper.updateByPrimaryKeySelective(user);
        if (dbResult > 0) {
            return ServerResponse.createBySuccessMessage("密码更新成功");
        }
        return ServerResponse.createByErroMessage("密码更新失败");
    }

    public ServerResponse<User> updateInformation(User user){
         int checkEmailResult = userMapper.checkEmailByUserId(user.getEmail(),user.getId());

        return null;
    }
}