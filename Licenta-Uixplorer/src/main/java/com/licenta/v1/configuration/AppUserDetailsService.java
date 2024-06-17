package com.licenta.v1.configuration;


import com.licenta.v1.models.AppUser;
import com.licenta.v1.models.Badge;
import com.licenta.v1.repositories.BadgeRepo;
import com.licenta.v1.repositories.UserRepo;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AppUserDetailsService implements UserDetailsService {


    private final UserRepo userRepo;
    private final BadgeRepo badgeRepo;
    private final CoordinatesService coordinatesService;


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Optional<AppUser> user = userRepo.findByEmail(email);
        if (user.isEmpty()) {
            throw new UsernameNotFoundException("User not found");
        }

        AppUser appUser = user.get();
        HttpSession session = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getSession();

        session.setAttribute("userId", appUser.getId());
        session.setAttribute("email", appUser.getEmail());
        session.setAttribute("name", appUser.getName());
        session.setAttribute("image", appUser.getImage());
        session.setAttribute("badges", appUser.getBadges());
        session.setAttribute("birthday", appUser.getBirthday());
        session.setAttribute("articlesRead", appUser.getArticlesRead());
        session.setAttribute("country", appUser.getCountry());
        session.setAttribute("state", appUser.getState());
        session.setAttribute("friendsList", appUser.getFriends());

        long currentDays = ChronoUnit.DAYS.between(appUser.getLastLoginDate(), LocalDate.now());
        if(currentDays == 1) {
            int newDays = appUser.getDays() + 1;

            if(newDays == 5){
                Optional<Badge> userBadge = badgeRepo.findById(7L);
                Badge badge = userBadge.get();
                if(!appUser.getBadges().contains(badge)){
                    appUser.getBadges().add(badge);
                }
            }
            if(newDays == 10){
                    Optional<Badge> userBadge = badgeRepo.findById(8L);
                    Badge badge = userBadge.get();
                    if(!appUser.getBadges().contains(badge)){
                        appUser.getBadges().add(badge);
                    }
            }
            if(newDays == 50){
                Optional<Badge> userBadge = badgeRepo.findById(9L);
                Badge badge = userBadge.get();
                if(!appUser.getBadges().contains(badge)){
                    appUser.getBadges().add(badge);
                }
            }
            if(newDays == 100){
                Optional<Badge> userBadge = badgeRepo.findById(10L);
                Badge badge = userBadge.get();
                if(!appUser.getBadges().contains(badge)){
                    appUser.getBadges().add(badge);
                }
            }

            appUser.setDays(newDays);
        }
        else if(currentDays > 1) {
            appUser.setDays(1);
        }

        String daysMsg;
        if(appUser.getDays() == 1){
            daysMsg = appUser.getDays() + " day";
        } else{
            daysMsg = appUser.getDays() + " days";
        }
        session.setAttribute("daysMessage", daysMsg);
        appUser.setLastLoginDate(LocalDate.now());

        return new User(appUser.getEmail(), appUser.getPassword(), getAuthorities(appUser));
    }

    private Collection<? extends GrantedAuthority> getAuthorities(AppUser appUser) {
        return List.of(new SimpleGrantedAuthority(appUser.getRole()));
    }


}
