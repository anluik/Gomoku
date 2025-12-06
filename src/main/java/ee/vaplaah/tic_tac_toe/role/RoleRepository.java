package ee.vaplaah.tic_tac_toe.role;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Repository
public interface RoleRepository extends ReactiveMongoRepository<Role, String> {

    Mono<Role> findByName(String roleName);

    Flux<Role> findAllByNameIn(Collection<String> names);
}
