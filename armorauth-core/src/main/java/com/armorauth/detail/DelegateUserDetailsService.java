package com.armorauth.detail;

import com.armorauth.data.repository.UserInfoRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.util.Assert;

import java.util.LinkedList;
import java.util.List;

public class DelegateUserDetailsService implements UserDetailsService {

    private List<UserDetailsService> userDetailsServices;

    public DelegateUserDetailsService(UserInfoRepository userInfoRepository) {
        this.userDetailsServices = new LinkedList<>();
        userDetailsServices.add(new JdbcCaptchaUserDetailsManager(userInfoRepository)::loadUserByPhone);
        userDetailsServices.add(new JdbcOAuth2UserDetailsManager(userInfoRepository)::loadOAuth2UserByUsername);
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Assert.notNull(username, "username cannot be null");
        for (UserDetailsService delegate : this.userDetailsServices) {
            UserDetails userDetails = delegate.loadUserByUsername(username);
            if (userDetails != null) {
                return userDetails;
            }
        }
        throw new UsernameNotFoundException("User not found");
    }

    public void setUserDetailsServices(List<UserDetailsService> userDetailsServices) {
        Assert.notEmpty(userDetailsServices, "userDetailsServices cannot be empty");
        this.userDetailsServices = userDetailsServices;
    }


}
