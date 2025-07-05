package com.jaywant.demo;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.jaywant.demo.Controller.CertificateController;
import com.jaywant.demo.Service.CertificateService;

@SpringBootTest
public class CertificateControllerTest {

    @Autowired
    private CertificateController certificateController;

    @Autowired
    private CertificateService certificateService;

    @Test
    public void testCertificateServiceInjection() {
        // Test that the CertificateService is properly injected
        assertNotNull(certificateService, "CertificateService should not be null");
        assertNotNull(certificateController, "CertificateController should not be null");
        
        System.out.println("✅ CertificateService injection test passed!");
    }
}
