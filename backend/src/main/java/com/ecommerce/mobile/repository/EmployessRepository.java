package com.ecommerce.mobile.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.ecommerce.mobile.entity.Employee;
import java.util.Optional;

@Repository
public interface EmployessRepository extends JpaRepository<Employee, Long>{
    Optional<Employee> findByEmail (String email);

    @Query("select e from Employee e where type(e) = Employee order by coalesce(e.hireDate, e.createdAt) desc")
    List<Employee> findAllStaff();

    @Query("select e from Employee e where e.userID = :id and type(e) = Employee")
    Optional<Employee> findStaffById(@Param("id") Long id);
}


