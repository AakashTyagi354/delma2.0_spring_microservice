package com.delma.doctorservice.serviceImpl;

import com.delma.doctorservice.Enum.ApplicationStatus;
import com.delma.doctorservice.client.UserServiceClient;
import com.delma.doctorservice.dto.DoctorApplicationRequest;
import com.delma.doctorservice.entity.Doctor;
import com.delma.doctorservice.kafka.NotificationEvent;
import com.delma.doctorservice.kafka.NotificationProducer;
import com.delma.doctorservice.repository.DoctorRepository;
import com.delma.doctorservice.service.DoctorService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserServiceClient userServiceClient;
    private final HttpServletRequest request;
    private final NotificationProducer notificationProducer;


    public void submitApplication(String userId, DoctorApplicationRequest request) {

        // Check if user already has a pending application
        boolean exists = doctorRepository.existsByUserIdAndStatus(userId, ApplicationStatus.PENDING);
        if (exists) {
            throw new RuntimeException("Pending application exists");
        }
        System.out.println("Hello in service impl");

        Doctor app = new Doctor();
        app.setUserId(userId);
        app.setSpecialization(request.getSpecialization());

        app.setExperience(request.getExperience());
        app.setStatus(ApplicationStatus.PENDING);
//        app.setAppliedAt(LocalDateTime.now());

        doctorRepository.save(app);
    }

    public void approveApplication(Long applicationId) {
        Doctor app = doctorRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        app.setStatus(ApplicationStatus.APPROVED);
        doctorRepository.save(app);
        // Extract JWT token from incoming request
        String token = request.getHeader("Authorization");
        // optionally call User MS to update ROLE_USER -> ROLE_DOCTOR
        userServiceClient.addDoctorRole(app.getUserId(),token);

        NotificationEvent event = new NotificationEvent(
                app.getUserId().toString(),
                "Doctor Application Approved",
                "Congratulations! Your application to become a doctor has been approved.",
                "Doctor"
        );
        notificationProducer.send(event);
    }

    public void rejectApplication(Long applicationId) {
        Doctor app = doctorRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        app.setStatus(ApplicationStatus.REJECTED);
        doctorRepository.save(app);
    }

    public List<Doctor> getPendingApplications() {
        return doctorRepository.findAllByStatus(ApplicationStatus.PENDING);
    }

    @Override
    public List<Doctor> getAllDoctors() {
        List<Doctor> allApprovedDoctors = doctorRepository.findAllByStatus(ApplicationStatus.APPROVED);
        if (allApprovedDoctors != null) {
            return allApprovedDoctors;
        }
        return List.of();
    }
}
