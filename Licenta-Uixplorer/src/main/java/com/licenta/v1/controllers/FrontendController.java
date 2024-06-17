package com.licenta.v1.controllers;

import com.licenta.v1.configuration.CommentUtil;
import com.licenta.v1.configuration.CoordinatesService;
import com.licenta.v1.models.*;
import com.licenta.v1.repositories.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;



@Controller
public class FrontendController {

    @Autowired
    UserRepo userRepo;

    @Autowired
    ArticleRepo articleRepo;

    @Autowired
    BadgeRepo badgeRepo;

    @Autowired
    DiscussionRepo discussionRepo;

    @Autowired
    CommentRepo commentRepo;

    @Autowired
    ChatRepo chatRepo;

    @Autowired
    MessageRepo messageRepo;

    @Autowired
    CoordinatesService coordinatesService;

    @Autowired
    private RestTemplate restTemplate;


    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    private AuthenticationManager authenticationManager;

    private String getFieldErrorMessage(BindingResult bindingResult, String fieldName) {
        if (bindingResult.hasFieldErrors(fieldName)) {
            return bindingResult.getFieldError(fieldName).getDefaultMessage();
        }
        return "";
    }

    @GetMapping("/")
    public String landing(Model model){

        long totalUsers = userRepo.count();
        long totalArticles = articleRepo.count();

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalArticles", totalArticles);


        return "landing";
    }

    @GetMapping("/homepage")
    public String homepage(Model model, HttpServletRequest request){

        HttpSession session = request.getSession();

        if(session != null) {

            Long userId = (Long) session.getAttribute("userId");
            String name = (String) session.getAttribute("name");
            String img = (String) session.getAttribute("image");
            String email = (String) session.getAttribute("email");
            String daysMessage = (String) session.getAttribute("daysMessage");
            Set<AppUser> friendsList = (Set<AppUser>) session.getAttribute("friendsList");
            if(friendsList.isEmpty()){
                model.addAttribute("hasfriends", false);
            } else{
                model.addAttribute("hasfriends", true);
            }

            session.setAttribute("discussions", discussionRepo.findAll());

            long totalUsers = userRepo.count();
            long totalArticles = articleRepo.count();

            model.addAttribute("totalUsers", totalUsers);
            model.addAttribute("totalArticles", totalArticles);

            model.addAttribute("daysMessage", daysMessage);
            model.addAttribute("userId", userId);
            model.addAttribute("name", name);
            model.addAttribute("image", img);
            model.addAttribute("message2", "Welcome back, " + name);


            model.addAttribute("friendsList", friendsList);

            AppUser currentUser = userRepo.findUserByEmail(email);
            List<Long> friendIds = currentUser.getFriends().stream().map(AppUser::getId).toList();
            List<AppUser> usersList = userRepo.findAll();
            usersList.removeIf(user -> friendIds.contains(user.getId()));
            usersList.remove(currentUser);

            int itemsPerPage = 3;
            int totalPages = (int) Math.ceil((double) usersList.size() / itemsPerPage);

            model.addAttribute("persons", usersList);
            model.addAttribute("totalPages", totalPages);


            Long chosenFriendId = (Long) model.getAttribute("chosenFriendId");
            model.addAttribute("chosenFriendId", chosenFriendId);

            List<AppUser> friendsRecomUsers = userRepo.findAll();

            friendsRecomUsers.forEach(user -> {
                if (user.getLatitude() == 0 && user.getLongitude() == 0) {
                    double[] coordinates = coordinatesService.getCoordinates(user.getCountry(), user.getState());
                    if (coordinates != null) {
                        user.setLatitude(coordinates[0]);
                        user.setLongitude(coordinates[1]);
                        userRepo.save(user);
                    }
                }
            });

            List<AppUser> friendsRecom = coordinatesService.getTop3ClosestUsers(userRepo.findAll(), currentUser);
            model.addAttribute("friendsRecom", friendsRecom);


            String url = "http://localhost:5000/recommend";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Long> requestBody = new HashMap<>();
            requestBody.put("user_id", userId);

            HttpEntity<Map<String, Long>> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Article[]> response;

            try {
                response = restTemplate.postForEntity(url, entity, Article[].class);
            } catch (Exception e) {
                model.addAttribute("error", "Could not get recommendations: " + e.getMessage());
                System.out.println("nu am putut afisa recomandari");
                return "homepage";
            }

            if (response.getBody() != null && response.getBody().length > 0) {
                List<Article> recommendedArticles = Arrays.asList(response.getBody());
                model.addAttribute("articlesRec", recommendedArticles);
            } else {
                model.addAttribute("articlesRec", Collections.emptyList());
                model.addAttribute("message3", "No recommendations available at the moment.");
                System.out.println("nicio recomandare disponibila");
            }

        }
        return "homepage";
    }

    @PostMapping("/add-friend")
    public String addFriend(@RequestParam("personId") Long personId, HttpServletRequest request, Model model){
        HttpSession session = request.getSession();

        String email = (String) session.getAttribute("email");
        String name = (String) session.getAttribute("name");
        String img = (String) session.getAttribute("image");
        String daysMessage = (String) session.getAttribute("daysMessage");
        model.addAttribute("name", name);
        model.addAttribute("image", img);
        model.addAttribute("daysMessage", daysMessage);

        AppUser user = userRepo.findUserByEmail(email);
        Optional<AppUser> friendOp = userRepo.findById(personId);
        AppUser friend = friendOp.get();

        user.getFriends().add(friend);
        friend.getFriends().add(user);

        Chat chat = new Chat();
        Chat chatFriend = new Chat();

        chat.setUser(user);
        chat.setFriend(friend);
        chat.setMessages(new ArrayList<>());
        chatRepo.save(chat);
        user.getChats().add(chat);

        chatFriend.setUser(friend);
        chatFriend.setFriend(user);
        chatFriend.setMessages(new ArrayList<>());
        chatRepo.save(chatFriend);
        friend.getChats().add(chatFriend);

        userRepo.save(user);
        userRepo.save(friend);

        Long chosenFriendId = (Long) model.getAttribute("chosenFriendId");
        model.addAttribute("chosenFriendId", chosenFriendId);

        session.setAttribute("friendsList", user.getFriends());
        model.addAttribute("friendsList", user.getFriends());

        return "redirect:/homepage";

    }


    @GetMapping("/register")
    public String showRegister(Model model){
        model.addAttribute("appUser", new AppUser());
        return "register";
    }


    @PostMapping("/register/save")
    public String register(@Valid AppUser appUser, Model model, HttpServletRequest request, BindingResult bindingResult, @RequestParam("imageFile") MultipartFile imgFile) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("emailError", getFieldErrorMessage(bindingResult, "email"));
            model.addAttribute("nameError", getFieldErrorMessage(bindingResult, "name"));
            model.addAttribute("passwordError", getFieldErrorMessage(bindingResult, "password"));
            return "register";
        }

        HttpSession session = request.getSession();
        Badge badge = badgeRepo.findBadgeById(1L);
        List<Badge> badgeList = new ArrayList<>();
        badgeList.add(badge);


        if(userRepo.findByEmail(appUser.getEmail()).isEmpty()){
            appUser.setPassword(bCryptPasswordEncoder.encode(appUser.getPassword()));
            appUser.setRegistrationDate(LocalDate.now());
            appUser.setLastLoginDate(LocalDate.now());
            appUser.setBadges(badgeList);


            if (!imgFile.isEmpty()) {
                try {
                    String uploadDirectory = "D:/Lavinia/LICENTA/Licenta-Proiect/Licenta-Uixplorer/src/main/resources/static/images";
                    String uploadDirectory2 = "D:/Lavinia/LICENTA/Licenta-Proiect/Licenta-Uixplorer/target/classes/static/images";
                    String fileName = StringUtils.cleanPath(Objects.requireNonNull(imgFile.getOriginalFilename()));
                    Path uploadPath = Paths.get(uploadDirectory);
                    Path uploadPath2 = Paths.get(uploadDirectory2);
                    Files.createDirectories(uploadPath);
                    Files.createDirectories(uploadPath2);
                    Path filePath = uploadPath.resolve(fileName);
                    Path filePath2 = uploadPath2.resolve(fileName);
                    Files.copy(imgFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                    Files.copy(imgFile.getInputStream(), filePath2, StandardCopyOption.REPLACE_EXISTING);
                    String imgPath = request.getContextPath() + "/images/" + fileName;
                    model.addAttribute("image", imgPath);
                    session.setAttribute("image", imgPath);

                    appUser.setImage(imgPath);
                } catch (IOException e) {
                    e.printStackTrace();
                    model.addAttribute("errorMessage", "Failed to upload image. Please try again.");
                    return "account";
                }
            }

            userRepo.save(appUser);

            double[] coordinates = coordinatesService.getCoordinates(appUser.getCountry(), appUser.getState());
            if (coordinates != null) {
                appUser.setLatitude(coordinates[0]);
                appUser.setLongitude(coordinates[1]);
                userRepo.save(appUser);
            }

            model.addAttribute("name", appUser.getName());
            model.addAttribute("image", appUser.getImage());
            model.addAttribute("email", appUser.getEmail());

            String daysMsg = appUser.getDays() + " day";
            model.addAttribute("daysMessage", daysMsg);

            session.setAttribute("daysMessage", daysMsg);
            session.setAttribute("appUser", appUser);
            session.setAttribute("name", appUser.getName());
            session.setAttribute("image", appUser.getImage());
            session.setAttribute("email", appUser.getEmail());
            session.setAttribute("badges", appUser.getBadges());


            return "login";
        }
        else return "register";
    }



    @GetMapping("/login")
    public String login(@RequestParam(value = "error", required = false) String error, Model model, HttpServletRequest request) {
        if (error != null) {
            model.addAttribute("errorMessage", "Invalid email or password");
        }
        return "login";
    }


    @GetMapping("/about")
    public String about(Model model, HttpServletRequest request){

        HttpSession session = request.getSession();

        String name = (String) session.getAttribute("name");
        String img = (String) session.getAttribute("image");
        String daysMessage = (String) session.getAttribute("daysMessage");
        Set<AppUser> friendsList = (Set<AppUser>) session.getAttribute("friendsList");

        if(friendsList.isEmpty()){
            model.addAttribute("hasfriends", false);
        } else{
            model.addAttribute("hasfriends", true);
        }
        model.addAttribute("friendsList", friendsList);

        model.addAttribute("name", name);
        model.addAttribute("image", img);
        model.addAttribute("message", "Hi, " + name + "!");
        model.addAttribute("daysMessage", daysMessage);


        return "about";
    }

    @GetMapping("/about-me")
    public String aboutMe(){
        return "about-me";
    }

    @GetMapping("/contact")
    public String contact(Model model, HttpServletRequest request){

        HttpSession session = request.getSession();

        String name = (String) session.getAttribute("name");
        String img = (String) session.getAttribute("image");
        String daysMessage = (String) session.getAttribute("daysMessage");
        Set<AppUser> friendsList = (Set<AppUser>) session.getAttribute("friendsList");

        if(friendsList.isEmpty()){
            model.addAttribute("hasfriends", false);
        } else{
            model.addAttribute("hasfriends", true);
        }
        model.addAttribute("friendsList", friendsList);

        model.addAttribute("name", name);
        model.addAttribute("image", img);
        model.addAttribute("daysMessage", daysMessage);

        return "contact";
    }

    @GetMapping("/contact-me")
    public String contactMe(){
        return "contact-me";
    }

    @GetMapping("/articles")
    public String articles(Model model, HttpServletRequest request, @RequestParam(defaultValue = "0") int page){

        HttpSession session = request.getSession();

        String name = (String) session.getAttribute("name");
        String img = (String) session.getAttribute("image");
        String daysMessage = (String) session.getAttribute("daysMessage");
        model.addAttribute("daysMessage", daysMessage);

        Pageable pageable = PageRequest.of(page, 9);
        Page<Article> articlesPage = articleRepo.findAll(pageable);
        model.addAttribute("articlesPage", articlesPage);

        Set<AppUser> friendsList = (Set<AppUser>) session.getAttribute("friendsList");

        if(friendsList.isEmpty()){
            model.addAttribute("hasfriends", false);
        } else{
            model.addAttribute("hasfriends", true);
        }
        model.addAttribute("friendsList", friendsList);

        session.setAttribute("articles", articleRepo.findAll());
        model.addAttribute("articles", articleRepo.findAll());
        model.addAttribute("name", name);
        model.addAttribute("image", img);

        model.addAttribute("searched", false);

        return "articles-catalog";
    }

    @GetMapping("/article")
    public String articleDetail(@PathParam("id") Long id, Model model, HttpServletRequest request){

        HttpSession session = request.getSession();

        Article article = articleRepo.findArticleById(id);

        String name = (String) session.getAttribute("name");
        String img = (String) session.getAttribute("image");
        String daysMessage = (String) session.getAttribute("daysMessage");
        model.addAttribute("daysMessage", daysMessage);

        Set<AppUser> friendsList = (Set<AppUser>) session.getAttribute("friendsList");

        if(friendsList.isEmpty()){
            model.addAttribute("hasfriends", false);
        } else{
            model.addAttribute("hasfriends", true);
        }
        model.addAttribute("friendsList", friendsList);

        model.addAttribute("name", name);
        model.addAttribute("image", img);
        model.addAttribute("article", article);

        return "article";
    }

    @PostMapping("/finishReading")
    public String finishReading(@RequestParam("articleId") Long articleId, HttpServletRequest request, Model model) {

        HttpSession session = request.getSession();

        Article article = articleRepo.findArticleById(articleId);

        String name = (String) session.getAttribute("name");
        String email = (String) session.getAttribute("email");
        String img = (String) session.getAttribute("image");
        String daysMessage = (String) session.getAttribute("daysMessage");
        model.addAttribute("daysMessage", daysMessage);

        model.addAttribute("name", name);
        model.addAttribute("image", img);
        model.addAttribute("article", article);

        AppUser user = userRepo.findUserByEmail(email);
        user.getArticlesRead().add(article);

        if(user.getArticlesRead().size() == 1){
            Badge badge = badgeRepo.findBadgeById(2L);
            user.getBadges().add(badge);
        }

        if(user.getArticlesRead().size() == 5){
            Badge badge = badgeRepo.findBadgeById(3L);
            user.getBadges().add(badge);
        }

        if(user.getArticlesRead().size() == 10){
            Badge badge = badgeRepo.findBadgeById(4L);
            user.getBadges().add(badge);
        }

        if(user.getArticlesRead().size() == 20){
            Badge badge = badgeRepo.findBadgeById(5L);
            user.getBadges().add(badge);
        }

        if(user.getArticlesRead().size() == 50){
            Badge badge = badgeRepo.findBadgeById(6L);
            user.getBadges().add(badge);
        }

        session.setAttribute("badges", user.getBadges());
        session.setAttribute("articlesRead", user.getArticlesRead());

        userRepo.save(user);


        return "redirect:/article?id=" + articleId;
    }


    @GetMapping("/search")
    @ResponseBody
    public String searchByTitle(@RequestParam("title") String title) {
        List<Article> searchedArticles = articleRepo.searchInTitle("%" + title + "%");
        StringBuilder sb = new StringBuilder();

        for (Article article : searchedArticles) {
            sb.append("<a href='/article?id=").append(article.getId()).append("'>");
            sb.append("<div class='articles__item'>");
            sb.append("<img src='").append(article.getMainImg()).append("'>");
            sb.append("<div class='overlay'></div>");
            sb.append("<div class='article__title'>");
            sb.append("<h5>").append(article.getTitle()).append("</h5>");
            sb.append("</div>");
            sb.append("</div>");
            sb.append("</a>");
        }

        return sb.toString();
    }

    @GetMapping("/account")
    public String showAccount(Model model, HttpServletRequest request){

        HttpSession session = request.getSession();

        String name = (String) session.getAttribute("name");
        String img = (String) session.getAttribute("image");
        String email = (String) session.getAttribute("email");
        String password = (String) session.getAttribute("password");
        String daysMessage = (String) session.getAttribute("daysMessage");

        Set<AppUser> friendsList = (Set<AppUser>) session.getAttribute("friendsList");

        if(friendsList.isEmpty()){
            model.addAttribute("hasfriends", false);
        } else{
            model.addAttribute("hasfriends", true);
        }
        model.addAttribute("friendsList", friendsList);

        model.addAttribute("daysMessage", daysMessage);
        model.addAttribute("name", name);
        model.addAttribute("image", img);
        model.addAttribute("email", email);
        model.addAttribute("password", password);

        return "account";
    }

    @PostMapping("/saveUserChanges")
    public String saveUserChanges(Model model, HttpServletRequest request,
                                  @RequestParam String email, @RequestParam String name,
                                  @RequestParam String currentPassword,
                                  @RequestParam String newPassword,
                                  @RequestParam("image") MultipartFile imgFile) {

        HttpSession session = request.getSession();
        String currentEmail = (String) session.getAttribute("email");

        if (currentEmail == null) {
            model.addAttribute("errorMessage", "No user is logged in.");
            return "login";
        }

        AppUser user = userRepo.findUserByEmail(currentEmail);

        if (user == null) {
            model.addAttribute("errorMessage", "User not found.");
            return "login";
        }

        if (email != null && !email.isEmpty()) {
            if (!email.equals(currentEmail)) {
                user.setEmail(email);
                session.setAttribute("email", email);
                model.addAttribute("email", email);
            }
        }

        if (name != null && !name.isEmpty()) {
            user.setName(name);
            model.addAttribute("name", name);
            session.setAttribute("name", name);
        }

        if (currentPassword != null && newPassword != null && !newPassword.isEmpty()) {
            String encodedPassword = userRepo.findPasswordByEmail(email);

            if (bCryptPasswordEncoder.matches(currentPassword, encodedPassword)) {
                String newPasswordEncoded = bCryptPasswordEncoder.encode(newPassword);
                user.setPassword(newPasswordEncoded);
            } else {
                model.addAttribute("errorMessage", "Incorrect current password.");
                return "account";
            }
        }

        if (!imgFile.isEmpty()) {
            try {
                String uploadDirectory = "D:/Lavinia/LICENTA/Licenta-Proiect/Licenta-Uixplorer/src/main/resources/static/images";
                String uploadDirectory2 = "D:/Lavinia/LICENTA/Licenta-Proiect/Licenta-Uixplorer/target/classes/static/images";
                String fileName = StringUtils.cleanPath(Objects.requireNonNull(imgFile.getOriginalFilename()));
                Path uploadPath = Paths.get(uploadDirectory);
                Path uploadPath2 = Paths.get(uploadDirectory2);
                Files.createDirectories(uploadPath);
                Files.createDirectories(uploadPath2);
                Path filePath = uploadPath.resolve(fileName);
                Path filePath2 = uploadPath2.resolve(fileName);
                Files.copy(imgFile.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
                Files.copy(imgFile.getInputStream(), filePath2, StandardCopyOption.REPLACE_EXISTING);
                String imgPath = request.getContextPath() + "/images/" + fileName;
                model.addAttribute("image", imgPath);
                session.setAttribute("image", imgPath);

                user.setImage(imgPath);
            } catch (IOException e) {
                e.printStackTrace();
                model.addAttribute("errorMessage", "Failed to upload image. Please try again.");
                return "account";
            }
        }


        userRepo.save(user);

        model.addAttribute("successMessage", "Profile updated successfully!");
        return "redirect:/account";
    }


    @GetMapping("/dashboard")
    public String showDashboard(Model model, HttpServletRequest request){

        HttpSession session = request.getSession();

        String name = (String) session.getAttribute("name");
        String img = (String) session.getAttribute("image");
        String email = (String) session.getAttribute("email");
        String daysMessage = (String) session.getAttribute("daysMessage");
        List<Badge> badges = (List<Badge>) session.getAttribute("badges");
        List<Article> articlesRead = (List<Article>) session.getAttribute("articlesRead");
        Set<AppUser> friendsList = (Set<AppUser>) session.getAttribute("friendsList");

        if(friendsList.isEmpty()){
            model.addAttribute("hasfriends", false);
        } else{
            model.addAttribute("hasfriends", true);
        }
        model.addAttribute("friendsList", friendsList);

        model.addAttribute("daysMessage", daysMessage);
        model.addAttribute("name", name);
        model.addAttribute("image", img);
        model.addAttribute("email", email);
        model.addAttribute("badges", badges);
        model.addAttribute("articlesRead", articlesRead);

        return "dashboard";

    }

    @GetMapping("/forum")
    public String showForum(Model model, HttpServletRequest request){

        HttpSession session = request.getSession();

        String name = (String) session.getAttribute("name");
        String img = (String) session.getAttribute("image");
        String email = (String) session.getAttribute("email");
        String daysMessage = (String) session.getAttribute("daysMessage");
        List<Discussion> discussions= (List<Discussion>) session.getAttribute("discussions");
        Set<AppUser> friendsList = (Set<AppUser>) session.getAttribute("friendsList");

        if(friendsList.isEmpty()){
            model.addAttribute("hasfriends", false);
        } else{
            model.addAttribute("hasfriends", true);
        }
        model.addAttribute("friendsList", friendsList);

        model.addAttribute("daysMessage", daysMessage);
        model.addAttribute("name", name);
        model.addAttribute("image", img);
        model.addAttribute("email", email);
        model.addAttribute("discussions", discussions);

        return "forum";
    }

    @GetMapping("/add-forum")
    public String showAddForum(Model model, HttpServletRequest request){

        HttpSession session = request.getSession();

        String name = (String) session.getAttribute("name");
        String img = (String) session.getAttribute("image");
        String email = (String) session.getAttribute("email");
        String daysMessage = (String) session.getAttribute("daysMessage");
        Set<AppUser> friendsList = (Set<AppUser>) session.getAttribute("friendsList");

        if(friendsList.isEmpty()){
            model.addAttribute("hasfriends", false);
        } else{
            model.addAttribute("hasfriends", true);
        }
        model.addAttribute("friendsList", friendsList);

        model.addAttribute("daysMessage", daysMessage);
        model.addAttribute("name", name);
        model.addAttribute("image", img);
        model.addAttribute("email", email);

        return "add-forum";
    }

    @PostMapping("/add-forum/save")
    public String addForumSave(Model model, HttpServletRequest request, Discussion discussion){

        HttpSession session = request.getSession();

        String name = (String) session.getAttribute("name");
        String img = (String) session.getAttribute("image");
        String email = (String) session.getAttribute("email");
        String daysMessage = (String) session.getAttribute("daysMessage");

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm");


        if(!discussion.getMessage().isEmpty()){
            LocalDateTime now = LocalDateTime.now();
            String time = now.format(dateTimeFormatter);
            discussion.setTimePosted(time);
            discussion.setUserImage(img);
            discussion.setUserName(name);

            discussionRepo.save(discussion);

            session.setAttribute("discussions", discussionRepo.findAll());

            return "redirect:/forum";
        }
        return "add-forum";
    }

    @PostMapping("/add-comment")
    public String addComment(Model mode, HttpServletRequest request, @RequestParam Long discussionId, @RequestParam String message){

        HttpSession session = request.getSession();

        String name = (String) session.getAttribute("name");
        String img = (String) session.getAttribute("image");
        String email = (String) session.getAttribute("email");
        String daysMessage = (String) session.getAttribute("daysMessage");

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yy HH:mm");

        if(!message.isEmpty()){

            Discussion discussion = discussionRepo.findById(discussionId).orElse(null);

            if(discussion != null) {

                LocalDateTime now = LocalDateTime.now();
                String time = now.format(dateTimeFormatter);

                Comment comment = new Comment();
                comment.setTimePosted(time);
                comment.setUserName(name);
                comment.setUserImage(img);
                comment.setMessage(CommentUtil.convertUrlsToLinks(message));
                comment.setDiscussion(discussion);
                commentRepo.save(comment);
                discussion.getCommentsList().add(comment);
                discussionRepo.save(discussion);
                session.setAttribute("discussions", discussionRepo.findAll());

                return "redirect:/forum";
            }
        }

        return "forum";
    }


}
