package com.erp.procurement.config;

<<<<<<< HEAD
import com.erp.procurement.entity.Supplier;
import com.erp.procurement.entity.User;
import com.erp.procurement.enums.Role;
import com.erp.procurement.repository.SupplierRepository;
import com.erp.procurement.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.logging.Logger;

@Component
public class DataSeeder implements CommandLineRunner {

    private static final Logger log = Logger.getLogger(DataSeeder.class.getName());

    private final UserRepository userRepository;
    private final SupplierRepository supplierRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository,
                      SupplierRepository supplierRepository,
                      PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.supplierRepository = supplierRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedSuppliers();
        log.info("═══════════════════════════════════════════════════════");
        log.info("  ERP PROCUREMENT SYSTEM — STARTED");
        log.info("  API Base URL : http://localhost:8080/api");
        log.info("  H2 Console   : http://localhost:8080/h2-console");
        log.info("  Default Logins:");
        log.info("    admin       / Admin@123   (ADMIN)");
        log.info("    coordinator / Coord@123   (PROCUREMENT_COORDINATOR)");
        log.info("    manager     / Mgr@123     (PURCHASING_MANAGER)");
        log.info("    finance     / Fin@123     (FINANCE)");
        log.info("═══════════════════════════════════════════════════════");
    }

    private void seedUsers() {
        createUser("admin",       "Admin User",    "admin@erp.com",    "Admin@123",  Role.ROLE_ADMIN);
        createUser("coordinator", "Paul Coord",    "paul@erp.com",     "Coord@123",  Role.ROLE_PROCUREMENT_COORDINATOR);
        createUser("manager",     "Sarah Owens",   "sowens@erp.com",   "Mgr@123",    Role.ROLE_PURCHASING_MANAGER);
        createUser("finance",     "Mark Rivera",   "mrivera@erp.com",  "Fin@123",    Role.ROLE_FINANCE);
    }

    private void createUser(String username, String fullName, String email, String rawPassword, Role role) {
        if (!userRepository.existsByUsername(username)) {
            userRepository.save(User.builder()
                    .username(username)
                    .fullName(fullName)
                    .email(email)
                    .password(passwordEncoder.encode(rawPassword))
                    .role(role)
                    .enabled(true)
                    .build());
        }
    }

    private void seedSuppliers() {
        createSupplier("SUP-001", "Zuckerman Security Ltd.", "Tom Bradley",   "tbradley@zuckerman.com", "214-555-0101", "Net 30");
        createSupplier("SUP-002", "Summit Traders Ltd.",     "Maria Gomez",   "mgomez@summit.com",      "210-555-0202", "Net 15");
        createSupplier("SUP-003", "MA Inc.",                 "Derek Chang",   "dchang@mainc.com",       "512-555-0303", "Net 30");
        createSupplier("SUP-004", "Ferguson HVAC",           "Carlos Vega",   "cvega@ferguson.com",     "800-555-0404", "Net 30");
        createSupplier("SUP-005", "Johnstone Supply",        "Patricia Hill", "phill@johnstone.com",    "888-555-0505", "Net 15");
    }

    private void createSupplier(String code, String name, String contact, String email, String phone, String terms) {
        if (!supplierRepository.existsBySupplierCode(code)) {
            supplierRepository.save(Supplier.builder()
                    .supplierCode(code)
                    .supplierName(name)
                    .contactPerson(contact)
                    .email(email)
                    .phone(phone)
                    .paymentTerms(terms)
                    .active(true)
                    .build());
        }
    }
}
=======
// DATA SEEDER DISABLED FOR DEMO
>>>>>>> 2f3462810bdb81884c007b6f64b6bb27d112e846
