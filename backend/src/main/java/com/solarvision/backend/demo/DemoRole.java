package com.solarvision.backend.demo;

/**
 * The demo accounts a visitor may sign in as with one click.
 * ADMIN is deliberately NOT in this list: nobody can become an administrator through the demo.
 */
public enum DemoRole {
    MANAGER(DemoDataSeeder.MANAGER_EMAIL),
    ENGINEER(DemoDataSeeder.ENGINEER_EMAIL),
    TECHNICIAN(DemoDataSeeder.TECHNICIAN_EMAIL),
    CUSTOMER(DemoDataSeeder.CUSTOMER_EMAIL);

    private final String email;

    DemoRole(String email) {
        this.email = email;
    }

    public String email() {
        return email;
    }
}
