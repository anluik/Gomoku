package ee.vaplaah.tic_tac_toe.user;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UserRepository extends ReactiveMongoRepository<User, String> {
    
}
