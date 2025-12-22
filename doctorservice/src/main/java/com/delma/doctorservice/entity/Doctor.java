package com.delma.doctorservice.entity;


import com.delma.doctorservice.Enum.ApplicationStatus;
import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDateTime;

@Entity
@Table(name = "doctors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Reference to User entity (optional)
    @Column(name = "user_id")
    private String userId;

//    @Column(nullable = false)
    private String firstName;

//    @Column(nullable = false)
    private String lastName;

//    @Column(nullable = false)
    private String password;

//    @Column(nullable = false)
    private String phone;

//    @Column(nullable = false, unique = true)
    private String email;

    private String website;

//    @ElementCollection
//    @CollectionTable(
//            name = "doctor_languages",
//            joinColumns = @JoinColumn(name = "doctor_id")
//    )
//    @Column(name = "language")
//    private List<String> languages;


    private String address;

//    @Column(nullable = false)
    private String specialization;

//    @Column(nullable = false)
    private Integer experience;

//    @Column(nullable = false)
    private Double feesPerConsultation;

//    @Column(nullable = false)
    private String gender;

//    @Enumerated(EnumType.STRING)
@Column(nullable = false)
    private ApplicationStatus status;

//    @Column(columnDefinition = "json")
//    private String timings;



}
