package com.licenta.v1.controllers;

import com.licenta.v1.models.AppUser;
import com.licenta.v1.models.Chat;
import com.licenta.v1.models.Message;
import com.licenta.v1.repositories.ArticleRepo;
import com.licenta.v1.repositories.ChatRepo;
import com.licenta.v1.repositories.MessageRepo;
import com.licenta.v1.repositories.UserRepo;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.websocket.server.PathParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController @RequestMapping
public class AppController {

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private ArticleRepo articleRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ChatRepo chatRepo;

    @Autowired
    private MessageRepo messageRepo;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/courses/all") ResponseEntity<Object> getAllCourses() {
        return ResponseEntity.ok(articleRepo.findAll());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/users/all")
    public ResponseEntity<Object> getAllUsers() {
        return ResponseEntity.ok(userRepo.findAll());
    }


    @GetMapping("/users/single")
    public ResponseEntity<Object> getMyDetails() {
        return ResponseEntity.ok(userRepo.findByEmail(getLoggedInUserDetails().getUsername()));
    }

    public UserDetails getLoggedInUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if(authentication != null && authentication.getPrincipal() instanceof UserDetails) {
            return (UserDetails) authentication.getPrincipal();
        }
        return null;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/articles/all")
    public ResponseEntity<Object> getAllArticles() {
        return ResponseEntity.ok(articleRepo.findAll());
    }



    @GetMapping("/chat/messages")
    public ResponseEntity<List<Message>> getChatMessages(@RequestParam("friendId") Long friendId, Model model) {

        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String email;
        if (principal instanceof UserDetails) {
            email = ((UserDetails) principal).getUsername();
        } else {
            email = principal.toString();
        }

        AppUser user = userRepo.findUserByEmail(email);

        Chat chat = chatRepo.findByUserAndFriend(user.getId(), friendId);

        List<Message> messages = chat.getMessages();

        return ResponseEntity.ok(messages);
    }

    @GetMapping("/get-friend-details")
    public ResponseEntity<Map<String, Object>> getFriendDetails(@RequestParam("friendId") Long friendId) {

        Optional<AppUser> friendOpt = userRepo.findById(friendId);
        if (friendOpt.isPresent()) {
            AppUser friend = friendOpt.get();
            Map<String, Object> friendDetails = new HashMap<>();
            friendDetails.put("name", friend.getName());
            friendDetails.put("image", friend.getImage());
            return ResponseEntity.ok(friendDetails);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping("/send-message")
    @ResponseBody
    public ResponseEntity<Message> sendMessage(@RequestParam("chosenFriendId") Long friendId, @RequestParam("chatmsg") String chatmsg, HttpServletRequest request) {

        HttpSession session = request.getSession();


        String email = (String) session.getAttribute("email");
        AppUser user = userRepo.findUserByEmail(email);
        Optional<AppUser> friendOp = userRepo.findById(friendId);
        AppUser friend = friendOp.orElseThrow(() -> new RuntimeException("Friend not found"));

        Chat chat = chatRepo.findByUserAndFriend(user.getId(), friendId);
        Chat friendChat = chatRepo.findByUserAndFriend(friendId, user.getId());

        Message message = new Message();
        Message friendMessage = new Message();

        message.setChat(chat);
        message.setPersonId(user.getId());
        message.setPersonImage(user.getImage());
        message.setMessage(chatmsg);

        friendMessage.setChat(friendChat);
        friendMessage.setPersonId(user.getId());
        friendMessage.setPersonImage(user.getImage());
        friendMessage.setMessage(chatmsg);

        messageRepo.save(message);
        messageRepo.save(friendMessage);

        chat.getMessages().add(message);
        friendChat.getMessages().add(friendMessage);

        chatRepo.save(chat);
        chatRepo.save(friendChat);


        return ResponseEntity.ok(message);
    }
}
