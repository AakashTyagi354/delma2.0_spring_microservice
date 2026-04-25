package com.delma.doctorservice.serviceImpl;

import com.delma.common.exception.ConflictException;
import com.delma.doctorservice.Enum.ApplicationStatus;
import com.delma.doctorservice.client.UserServiceClient;
import com.delma.doctorservice.dto.DoctorApplicationRequest;
import com.delma.doctorservice.dto.DoctorResponse;
import com.delma.doctorservice.entity.Doctor;
import com.delma.doctorservice.kafka.NotificationEvent;
import com.delma.doctorservice.kafka.NotificationProducer;
import com.delma.doctorservice.repository.DoctorRepository;
import com.delma.doctorservice.service.DoctorService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.ResourceNotFoundException;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import javax.naming.ConfigurationException;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final DoctorRepository doctorRepository;
    private final UserServiceClient userServiceClient;
    private final HttpServletRequest httpServletRequest;
    private final NotificationProducer notificationProducer;


    @Override
    @Transactional
    public void submitApplication(String userId, DoctorApplicationRequest request) {

        log.info("Doctor application received for userId: {}", userId);


        boolean exists = doctorRepository.existsByUserIdAndStatus(userId, ApplicationStatus.PENDING);
        if (exists) {
            throw new ConflictException("A pending application already exists for user: " + userId);
        }

        Doctor app = Doctor.builder()
                        .userId(userId)
                        .specialization(request.getSpecialization())
                                .experience(request.getExperience())
                                        .status(ApplicationStatus.PENDING)
                                                .build();


        doctorRepository.save(app);
        log.info("Doctor application saved successfully for userId: {}", userId);
    }


    @Override
    @Transactional
    public void approveApplication(Long userId) {
        Doctor app = doctorRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor application not found with id: " + userId));

        app.setStatus(ApplicationStatus.APPROVED);
        doctorRepository.save(app);
        // Extract JWT token from incoming request
        String token = httpServletRequest.getHeader("Authorization");
        // optionally call User MS to update ROLE_USER -> ROLE_DOCTOR
        userServiceClient.addDoctorRole(app.getUserId(), token);

        NotificationEvent event = new NotificationEvent(
                app.getUserId().toString(),
                "Doctor Application Approved",
                "Congratulations! Your application to become a doctor has been approved.",
                "Doctor"
        );
        notificationProducer.send(event);
        log.info("Doctor application approved for id: {}", userId);
    }

    @Override
    @Transactional
    public void rejectApplication(Long applicationId) {
        Doctor app = doctorRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Doctor application not found with id: " + applicationId));
        app.setStatus(ApplicationStatus.REJECTED);
        doctorRepository.save(app);
        log.info("Doctor application rejected for id: {}", applicationId);
    }

    @Override
    public List<DoctorResponse> getPendingApplications() {

        return doctorRepository.findAllByStatus(ApplicationStatus.PENDING).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<DoctorResponse> getAllDoctors() {
        // FIXED: removed null check — Spring Data JPA NEVER returns null from findAll queries
        // It always returns an empty list [] if nothing is found
        return doctorRepository.findAllByStatus(ApplicationStatus.APPROVED).stream()
                .map(this::toResponse)
                .toList();

    }

    @Override
    public List<DoctorResponse> getAllPendingDoctors() {
        return doctorRepository.findAllByStatus(ApplicationStatus.PENDING)
                .stream()
                .map(this::toResponse)
                .toList();

    }

    @Override
    public List<DoctorResponse> searchDoctors(String keyword) {
        log.info("Searching for doctors with keyword: {}", keyword);

        // FIXED: status filter moved INTO the database query
        // LESSON — Why this matters:
        // OLD: fetch ALL matching doctors → load into Java memory → filter in Java
        //      If 10,000 doctors match "john", all 10,000 loaded into RAM
        // NEW: database filters by status AND keyword together
        //      Only APPROVED doctors matching keyword come back — DB does the work
        // Rule: always filter as close to the data source as possible
//        try {
//
//            List<Doctor> searchResults =
//                    doctorRepository.findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCaseOrSpecializationContainingIgnoreCase(keyword, keyword, keyword);
//
//            return searchResults.stream()
//                    .filter(doctor -> doctor.getStatus() == ApplicationStatus.APPROVED)
//                    .collect(Collectors.toList());
//        } catch (DataAccessException ex) {
//            log.info("Database error while searching for doctors: {}", ex.getMessage());
//            throw new RuntimeException("Database error occurred while searching for doctors", ex);
//        } catch (Exception ex) {
//            throw new RuntimeException("An unexpected error occurred while searching for doctors", ex);
//        }

        return doctorRepository.findByStatusAndFirstNameContainingIgnoreCaseOrSpecializationContainingIgnoreCase(
                ApplicationStatus.PENDING, keyword, keyword
        )
                .stream()
                .map(this::toResponse)
                .toList();


    }

    private DoctorResponse toResponse(Doctor doctor) {
        return DoctorResponse.builder()
                .id(doctor.getId())
                .userId(doctor.getUserId())
                .firstName(doctor.getFirstName())
                .lastName(doctor.getLastName())
                .email(doctor.getEmail())
                .phone(doctor.getPhone())
                .specialization(doctor.getSpecialization())
                .experience(doctor.getExperience())
                .feesPerConsultation(doctor.getFeesPerConsultation())
                .gender(doctor.getGender())
                .address(doctor.getAddress())
                .website(doctor.getWebsite())
                .status(doctor.getStatus())
                .build();
    }
}
