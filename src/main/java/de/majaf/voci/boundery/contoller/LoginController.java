package de.majaf.voci.boundery.contoller;

import de.majaf.voci.control.service.IUserService;
import de.majaf.voci.control.service.exceptions.user.UserAlreadyExistsException;
import de.majaf.voci.entity.RegisteredUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class LoginController {

    @Autowired
    private IUserService userService;

    @RequestMapping(value = {"/", "/login"}, method = RequestMethod.GET)
    public String prepareLoginPage(Model model) {
        model.addAttribute("usernameOrPwWrong", false);
        return "login";
    }

    @RequestMapping(value = "/failedLogin", method = RequestMethod.GET)
    public String prepareFailedLoginPage(Model model) {
        model.addAttribute("usernameOrPwWrong", true);
        return "login";
    }

    @RequestMapping(value = "/register", method = RequestMethod.GET)
    public String prepareRegisterPage(Model model) {
        model.addAttribute("registeredUser", new RegisteredUser());
        model.addAttribute("errorMsg", "");
        return "register";
    }

    @RequestMapping(value = "/register", method = RequestMethod.POST)
    public String registerNewUser(Model model, @ModelAttribute RegisteredUser registeredUser) {
        try {
            userService.registerUser(registeredUser);
            model.addAttribute("successMsg", "User " + registeredUser.getUserName() + " was created!");
            return "login";
        } catch (UserAlreadyExistsException uaee) {
            model.addAttribute("errorMsg", "User already exists");
        }
        return "register";
    }

}
