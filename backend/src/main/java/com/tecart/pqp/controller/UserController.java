package com.tecart.pqp.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tecart.pqp.entity.base.User;
import com.tecart.pqp.service.CommonService;
import com.tecart.pqp.service.UserService;
import com.tecart.pqp.utils.constants.MasterConstants;
import com.tecart.pqp.utils.exceptions.RecordNotFoundException;

@RestController
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequestMapping(path = MasterConstants.ROOT_API_PATH)
public class UserController {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
	private UserService userService;

	@Autowired
	private CommonService commonService;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@PreAuthorize("hasAnyAuthority('READ_USER', 'SUPPER_ADMIN_API', 'ADMIN_API')")
	@GetMapping({ "/users" })
	public Page<User> getListOfAllUser(
			@RequestParam(value = "pageNo", defaultValue = MasterConstants.DEFAULT_PAGE_NO_VALUE) Integer pageNo,
			@RequestParam(value = "sortKey", defaultValue = MasterConstants.DEFAULT_SORT_KEY) String sortKey,
			@RequestParam(value = "recordsPerPage", defaultValue = MasterConstants.DEFAULT_RECORDS_PER_PAGE_VALUE) Integer recordsPerPage,
			@RequestParam(value = "sortDir", defaultValue = MasterConstants.DEFAULT_SORT_DIR) String sortDir,
			@RequestParam(value = "body") String userParam) throws JsonProcessingException {
		logger.info("Inside getListOfAllUser method in controller");

		ObjectMapper objectMapper = new ObjectMapper();
		User user = objectMapper.readValue(userParam, User.class);
		User currentLoggedInUser = commonService.getCurrentUserDetails();

		return userService.getListOfAllUser(user, pageNo, sortKey, recordsPerPage, sortDir, currentLoggedInUser);

	}

	@PreAuthorize("hasAnyAuthority('CREATE_USER', 'SUPPER_ADMIN_API', 'ADMIN_API')")
	@PostMapping(path = { "/users" })
	public User saveOrUpdateUser(@RequestBody User user) {
		logger.info("Inside saveOrUpdateUser method in controller");

		User currentLoggedInUser = commonService.getCurrentUserDetails();
		String userName = commonService.getUserNameByLoggedInUser(currentLoggedInUser);
		
		user.setCreatedBy(userName);
		if (user.getId() > 0) {
			user.setUpdatedBy(userName);
			user.setPassword(commonService.isGivenStringNtEmptyAndNtNull(user.getPassword()) ? user.getPassword()
					: commonService.getPasswordByUserId(user.getId()));
		} else {
			user.setUpdatedBy(userName);
			String encodedPassword = passwordEncoder.encode(user.getPassword());
			user.setPassword(encodedPassword);

		}

		return userService.saveOrUpdateUser(user);
	}
	
	@PreAuthorize("hasAnyAuthority('READ_USER', 'COMMON_API')")
	@GetMapping(path = { "/users/{id}" })
	public User loadUserById(@PathVariable(required = false) String erpOrgCode, @PathVariable Integer id,
			@PathVariable(required = false) String erpEntityCode) {
		logger.info("Inside loadUserById method in controller");
		
		User currentLoggedInUser = commonService.getCurrentUserDetails();

		User user = userService.loadUserById(id, currentLoggedInUser);

		if (user == null) {
			String recordNotFoundMessage = "User not found for User Id - " + id;
			logger.info(recordNotFoundMessage);
			throw new RecordNotFoundException(recordNotFoundMessage);
		}

		return user;
	}

	@PreAuthorize("hasAnyAuthority('DELETE_USER', 'SUPPER_ADMIN_API', 'ADMIN_API')")
	@DeleteMapping(path = { "/users/{id}"})
	public void deleteUserById(@PathVariable(required = false) String erpOrgCode, @PathVariable Integer id) {
		logger.info("Inside deleteUserById method in controller");

		User currentLoggedInUser = commonService.getCurrentUserDetails();
		userService.deleteUserById(id, currentLoggedInUser);
	}
}
