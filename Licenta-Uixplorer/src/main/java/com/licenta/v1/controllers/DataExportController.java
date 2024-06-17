package com.licenta.v1.controllers;


import com.licenta.v1.models.AppUser;
import com.licenta.v1.models.Article;
import com.licenta.v1.models.UserArticle;
import com.licenta.v1.models.UserMapInfo;
import com.licenta.v1.repositories.ArticleRepo;
import com.licenta.v1.repositories.UserArticleRepo;
import com.licenta.v1.repositories.UserRepo;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
public class DataExportController {

    private final ArticleRepo articleRepo;

    private final UserArticleRepo userArticleRepo;

    private final UserRepo userRepo;

    public DataExportController(ArticleRepo articleRepo, UserArticleRepo userArticleRepo, UserRepo userRepo) {
        this.articleRepo = articleRepo;
        this.userArticleRepo = userArticleRepo;
        this.userRepo = userRepo;
    }

    @GetMapping("/articles")
    public List<Article> getAllArticles() {
        return articleRepo.findAll();
    }

    @GetMapping("/users/{userId}/articlesRead")
    public List<Article> getArticlesReadByUser(@PathVariable Long userId) {
        AppUser user = userRepo.findById(userId).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return user.getArticlesRead();
    }

    @GetMapping("/user_articles")
    public List<UserArticle> getAllUserArticles() {
        return userArticleRepo.findAll();
    }


    @GetMapping("/users")
    public List<UserMapInfo> getAllUsersHome() {
        List<AppUser> users = userRepo.findAll();
        List<UserMapInfo> userInfoList = new ArrayList<>();

        for (AppUser user : users) {
            UserMapInfo userInfo = new UserMapInfo();
            userInfo.setId(user.getId());
            userInfo.setName(user.getName());
            userInfo.setLatitude(user.getLatitude());
            userInfo.setLongitude(user.getLongitude());
            userInfoList.add(userInfo);
        }

        return userInfoList;
    }
}