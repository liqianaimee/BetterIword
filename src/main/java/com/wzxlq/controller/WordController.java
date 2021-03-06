package com.wzxlq.controller;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wzxlq.dto.WordInfoDTO;
import com.wzxlq.entity.*;
import com.wzxlq.exception.QueryWordException;
import com.wzxlq.service.UserService;
import com.wzxlq.service.WordService;
import com.wzxlq.utils.sentUtil;
import com.wzxlq.vo.QueryAllVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

/**
 * (Word)表控制层
 *
 * @author makejava
 * @since 2020-04-13 21:49:20
 */
@Slf4j
@RestController
//@RequestMapping("word")
public class WordController {
    /**
     * 服务对象
     */
    @Resource
    private WordService wordService;

    @Resource
    private UserService userService;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    //    @Transactional
    @GetMapping("queryAll")
    public QueryAllVO queryAll(HttpServletRequest request) {
        //获取code
        String code = request.getParameter("code");
        System.out.println(code);
        //换取accesstoken的地址
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=APPID&secret=SECRET&code=CODE&grant_type=authorization_code";
        url = url.replace("APPID", "wxb1b153e1c472f2de")
                .replace("SECRET", "2a7de949357073700a095c4b86b4c290")
                .replace("CODE", code);
        String result = sentUtil.get(url);
        JsonParser parser = new JsonParser();
        JsonElement root = parser.parse(result);
        JsonObject json = root.getAsJsonObject();
        String at = json.get("access_token").getAsString();
        String openId = json.get("openid").getAsString();
        //拉取用户的基本信息
        url = "https://api.weixin.qq.com/sns/userinfo?access_token=ACCESS_TOKEN&openid=OPENID&lang=zh_CN";
        url = url.replace("ACCESS_TOKEN", at).replace("OPENID", openId);
        result = sentUtil.get(url);
        Gson gson = new Gson();
        wxUser wxUser = gson.fromJson(result, wxUser.class);
        System.out.println(wxUser.getHeadimgurl());
        System.out.println(result);
        User user = userService.queryById(openId);
        if (user == null) {
            List<Word> words = wordService.firstQueryWords(openId);
            if (words == null || words.isEmpty()) {
                log.error("第一次查询不到单词");
                throw new QueryWordException("查不到单词", 500);
            }
            //用户第一次进入，插入用户信息到数据库
            userService.insert(new User(openId, wxUser.getNickname(), 1, wxUser.getHeadimgurl()));
            //默认用户设置每日提醒背单词
            redisTemplate.opsForHash().put("User_" + openId, "isTixing", 1);
            return new QueryAllVO(openId, words);
        } else {
            List<Word> words = wordService.queryTodayWords(openId);
            return new QueryAllVO(openId, words);
        }
    }
    @GetMapping("queryWord")
    public List<Word> queryWord(){
        List<Word> wordList = wordService.queryAllByLimit(1080, 10);
        return wordList;
    }

    @GetMapping("queryInTest")
    public QueryAllVO queryInTest(String openId) {
        System.out.println(openId);
        User user = userService.queryById(openId);
        if (user == null) {
            List<Word> words = wordService.firstQueryWords(openId);
            if (words == null || words.isEmpty()) {
                log.error("第一次查询不到单词");
                throw new QueryWordException("查不到单词", 500);
            }
            userService.insert(new User(openId, openId + "测", 1, "http://wework.qpic.cn/bizmail/bia8Sib0kGlNZbysx3LwoGou727nT1ibdY12POO95bXoIU9SibcrffqFjg/0"));
            //默认用户设置每日提醒背单词
            redisTemplate.opsForHash().put("User_" + openId, "isTixing", 1);
            return new QueryAllVO(openId, words);
        } else {
            List<Word> words = wordService.queryTodayWords(openId);
            return new QueryAllVO(openId, words);
        }
    }

    @GetMapping("/word/queryInEs")
    public List<Map<String, Object>> queryInEs(String keyword) {
        return wordService.queryInEs(keyword);
    }

    //每次点击都要经过这个统计
    @PostMapping("/word/wordInfo")
    public boolean wordInfo(@RequestBody WordInfoDTO wordInfoDTO, HttpServletRequest request) {
        String openId = request.getHeader("token");
        boolean success = wordService.wordInfo(wordInfoDTO, openId);
        return success;
    }

    //今日任务完成
    @GetMapping("/word/finishToday")
    public boolean finishToday(HttpServletRequest request) {
        String openId = request.getHeader("token");
        return wordService.killWordMapWithOpenId(openId);
    }

    //复习
    @GetMapping("/word/review")
    public List<Word> review(HttpServletRequest request) {
        String openId = request.getHeader("token");
        return wordService.review(openId);
    }
    //词汇量测试
    @GetMapping("/word/wordCountTest")
    public List<Word> wordCountTest() {
        return  wordService.wordCountTest();
    }
}