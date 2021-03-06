package com.linkedin.features.jobs;

import com.linkedin.config.errors.NotAuthorizedException;
import com.linkedin.config.errors.ObjectNotFoundException;
import com.linkedin.config.security.AuthenticationFacade;
import com.linkedin.entities.Job;
import com.linkedin.entities.JobApplication;
import com.linkedin.entities.Login;
import com.linkedin.entities.repo.JobApplicationRepository;
import com.linkedin.entities.repo.JobRepository;
import com.linkedin.entities.repo.UserRepository;
import com.linkedin.features.users.UserConverter;
import com.linkedin.model.UserSimpleDto;
import com.linkedin.model.jobApplication.JobApplicationDto;
import com.linkedin.model.jobs.JobDto;
import com.linkedin.model.jobs.JobRequestDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class JobService {
  private final JobRepository jobRepository;
  private final JobConverter jobConverter;
  private final JobApplicationRepository jobApplicationRepository;
  private final JobApplicationConverter jobApplicationConverter;
  private final UserRepository userRepository;
  private final UserConverter userConverter;

  @Autowired
  public JobService(JobRepository jobRepository, JobConverter jobConverter, JobApplicationRepository jobApplicationRepository, JobApplicationConverter jobApplicationConverter, UserRepository userRepository,
      UserConverter userConverter) {
    this.jobRepository = jobRepository;
    this.jobConverter = jobConverter;
    this.jobApplicationRepository = jobApplicationRepository;
    this.jobApplicationConverter = jobApplicationConverter;
    this.userRepository = userRepository;
    this.userConverter = userConverter;
  }

  public List<JobDto> getAllJobUserCreated(Long userId) {
    return jobRepository.findAllByAuthorId(userId).stream().map(jobConverter::toJobDto).collect(Collectors.toList());
  }

  public Job createJob(JobRequestDto dto) {
    Job job = new Job();
    job.setTitle(dto.getTitle());
    job.setCompany(dto.getCompany());
    job.setAuthorId(AuthenticationFacade.getUserId());
    job.setDescription(dto.getDescription());
    job.setDate(new Date());
    job.setSkills(dto.getSkills());

    jobRepository.save(job);

    return job;
  }

  public List<JobDto> getAllJobs() {
    return jobRepository.findAll()
        .stream()
        .map(jobConverter::toJobDto)
        .collect(Collectors.toList());
  }

  public void removeJob(Long jobId) throws Exception {

    if (existsJob(jobId)) {
      //we check if the User that tries to erase the Job is re author of the Job
      Login login = AuthenticationFacade.authenticatedUser();
      Long userId = login.getUserId();

      Job job = jobRepository.findById(jobId).orElse(null);
      if (job.getAuthorId() != userId) {
        throw new NotAuthorizedException(Job.class);
      }

      jobRepository.deleteById(jobId);
    } else {
      throw new ObjectNotFoundException(Job.class, jobId);

    }


  }

  public JobDto updateJob(Long jobId, JobRequestDto jobRequestDto) throws Exception {

    ///first we check if the job exists in our database
    //we check if the User that tries update the Job is re author of the Job
    if (existsJob(jobId)) {

      Login login = AuthenticationFacade.authenticatedUser();
      Long userId = login.getUserId();

      Job job = jobRepository.findById(jobId).orElse(null);
      if (job.getAuthorId() != userId) {
        throw new NotAuthorizedException(Job.class);
      }

      //job = new Job();
      job.setJobId(jobId);
      job.setCompany(jobRequestDto.getCompany());
      job.setDescription(jobRequestDto.getDescription());
      job.setTitle(jobRequestDto.getTitle());
      job.setSkills(jobRequestDto.getSkills());

      jobRepository.save(job);
      return jobConverter.toJobDto(job);


    } else {
      throw new ObjectNotFoundException(Job.class, jobId);
    }


  }

  public boolean existsJob(Long jobId) {
    return jobRepository.existsById(jobId);
  }

  public JobDto getJob(Long jobId) throws Exception {

    return jobRepository.findById(jobId)
        .map(jobConverter::toJobDto)
        .orElseThrow(() -> new ObjectNotFoundException(Job.class, jobId));
  }

  public JobApplicationDto applyToJob(Long jobId) throws Exception {
    if (!jobRepository.existsById(jobId)) {
      throw new ObjectNotFoundException(Job.class, jobId);
    }
    Long userId = AuthenticationFacade.authenticatedUser().getUserId();

    JobApplication jobApplication = new JobApplication();
    jobApplication.setJobId(jobId);
    jobApplication.setUserId(userId);

    jobApplicationRepository.save(jobApplication);
    return jobApplicationConverter.toJobApplicationDto(jobApplication);

  }

  public List<JobApplicationDto> getJobApplications() {
    return jobApplicationRepository.findAll().stream().map(jobApplicationConverter::toJobApplicationDto).collect(Collectors.toList());
  }

  public List<JobApplicationDto> getyMJobApplications() {
    Long userId = AuthenticationFacade.authenticatedUser().getUserId();
    return jobApplicationRepository.findAllByUserId(userId)
        .stream()
        .map(jobApplicationConverter::toJobApplicationDto)
        .collect(Collectors.toList());
  }

  public List<UserSimpleDto> getyMJobApplicants(Long jobId) {
    Long userId = AuthenticationFacade.authenticatedUser().getUserId();
    //List<JobApplication> jobApplicationList =  jobApplicationRepository.findAllByJobId(jobId);
    return jobApplicationRepository.findAllByJobId(jobId)
        .stream()
        .map(x -> userConverter.toUserSimpleDto(userRepository.findById(x.getUserId()).orElse(null)))
        .collect(Collectors.toList());
  }
}
