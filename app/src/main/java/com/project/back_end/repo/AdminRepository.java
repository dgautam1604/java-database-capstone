package com.project.back_end.repo;

import com.project.back_end.models.Admin;  // Import Admin model class
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Long> {

    // Custom query method to find an Admin by their username
    Admin findByUsername(String username);
}
