package info.keeper.controllers;

import info.keeper.models.Contact;
import info.keeper.models.User;
import info.keeper.repositories.ContactRepository;
import info.keeper.repositories.UserRepository;
import info.keeper.utils.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import javax.validation.Valid;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping(value = "/users", method = RequestMethod.GET) // for user
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContactRepository contactRepository;

    @GetMapping
    public String dashboard(Model model, Principal principal) {
        String username = principal.getName();
        //get user using username
        User user = userRepository.findUserByUsername(username);

        model.addAttribute("title", "User Dashboard Page - Information Keeper");

        model.addAttribute("user", user);
        return "normal_user/user_dashboard";
    }


    // show add Note form
    @GetMapping("/addNote")
    public String showAddContactForm(Model model, Principal principal) {
        String username = principal.getName();
        User user = userRepository.findUserByUsername(username);
        model.addAttribute("title", "Add New Information || Information Keeper");
        model.addAttribute("contact", new Contact());
        model.addAttribute("user", user);
        return "normal_user/add_note";
    }

    // add note to db
    // ORDER MATTERS !!! Place BindingResult after ModelAttribute
    @PostMapping("/addContact")
    public String addContact(@Valid @ModelAttribute("contact") Contact contact, BindingResult result,
                             Model model, HttpSession session, Principal principal,
                             @RequestParam("profileImage") MultipartFile file) {

        if (result.hasErrors()) {
            //System.out.println("error is " + result.getAllErrors().toString());
            model.addAttribute("contact", contact);
            return "normal_user/add_note";
        }
        try {
            User user = userRepository.findUserByUsername(principal.getName());

            // uploading file
            if (!file.isEmpty()) {
                //upload file to images folder and save name to database
                contact.setImageURL(user.getId() + "_" + file.getOriginalFilename());
                //System.out.println("\n original file name : " + file.getOriginalFilename() + " contact id : " + contact.getId());
                File folderToSaveFile = new ClassPathResource("static/images").getFile();
                Path path = Paths.get(folderToSaveFile.getAbsolutePath() + File.separator + user.getId() + "_" + file.getOriginalFilename());
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            }
            // bi-directional mapping
            contact.setUser(user); // set user to this contact
            user.getContacts().add(contact);// add this contact to the user list

            userRepository.save(user);// update the user
            model.addAttribute("contact", new Contact());
            session.setAttribute("message", new Message("New information has been added", "alert-success"));
        } catch (Exception e) {
            //e.printStackTrace();
            session.setAttribute("message", new Message("Something Went Wrong : " + e.getMessage(), "alert-danger"));
            model.addAttribute("contact", contact);
            return "normal_user/add_note";
        }
        model.addAttribute("contact", new Contact());
        return "normal_user/add_note";
    }

    //list all of your notes
    // handle also pagination
    // per page = 5 notes
    @GetMapping("/notes/{pageNumber}")
    public String findContactsByUser(@PathVariable("pageNumber") int pageNumber, Pageable pageable,
                                     Model model, Principal principal) {

        User user = userRepository.findUserByUsername(principal.getName());

       //Pageable has current page and number of notes per page
        Pageable pageable1 = PageRequest.of(pageNumber, 2); // 5 per page
        Page<Contact> allContacts = contactRepository.findContactsByUser(user.getId(), pageable1);

        model.addAttribute("title", "All Of your Notes || Information Keeper");
        model.addAttribute("user", user);
        model.addAttribute("contacts", allContacts);
        model.addAttribute("currentPage", pageNumber);
        model.addAttribute("totalPages", allContacts.getTotalPages());

        return "normal_user/all_notes";
    }
}