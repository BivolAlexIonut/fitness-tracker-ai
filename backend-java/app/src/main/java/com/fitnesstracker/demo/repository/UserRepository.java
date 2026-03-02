package com.fitnesstracker.demo.repository;

import com.fitnesstracker.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Faptul că moștenește JpaRepository îți dă instantaneu comenzi precum .save(), .findAll(), .findById()
}