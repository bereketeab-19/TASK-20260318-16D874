package com.petsupplies.user.repo;

import com.petsupplies.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByUsername(String username);

  @Query("SELECT DISTINCT u.merchantId FROM User u WHERE u.merchantId IS NOT NULL")
  List<String> findDistinctMerchantIds();
}

