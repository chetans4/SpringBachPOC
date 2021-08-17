package com.chetan.sbatch.config;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import com.chetan.sbatch.entity.User;
import com.chetan.sbatch.processor.UserProcessor;


@Configuration
@EnableAutoConfiguration
@EnableBatchProcessing
public class SpringBatchConfig {

	@Autowired
	private StepBuilderFactory stepFactory;
	
	@Autowired
	private JobBuilderFactory jobFactory;
	
	@Bean
	public FlatFileItemReader<User> reader() {
		return new FlatFileItemReaderBuilder<User>()
			.name("userItemReader")
			.resource(new ClassPathResource("sample-data.csv"))
			.delimited()
			.names(new String[]{"firstName", "lastName"})
			.fieldSetMapper(new BeanWrapperFieldSetMapper<User>() {{
				setTargetType(User.class);
			}})
			.build();
	}

	@Bean
	public UserProcessor processor() {
		return new UserProcessor();
	}
	
	@Bean
	public JdbcBatchItemWriter<User> writer(DataSource dataSource) {
		return new JdbcBatchItemWriterBuilder<User>()
			.itemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<>())
			.sql("INSERT INTO people (first_name, last_name) VALUES (:firstName, :lastName)")
			.dataSource(dataSource)
			.build();
	}
	
	@Bean
	public Step step1(JdbcBatchItemWriter<User> writer) {
		return stepFactory.get("step1")
			.<User, User> chunk(10)
			.reader(reader())
			.processor(processor())
			.writer(writer)
			.build();
	}
	
	@Bean
	public Job importUserJob(JobCompletionNotificationListener listener, Step step1) {
		return jobFactory.get("importUserJob")
			.incrementer(new RunIdIncrementer())
			.listener(listener)
			.flow(step1)
			.end()
			.build();
	}
	
	private JobRepository getJobRepository(DataSource dataSource) throws Exception {
		System.out.println("get job repository : "+dataSource);
        JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
        factory.setDataSource(dataSource);
        factory.setTransactionManager(getTransactionManager());
        factory.afterPropertiesSet();
        return (JobRepository) factory.getObject();
    }

    private PlatformTransactionManager getTransactionManager() {
        return new ResourcelessTransactionManager();
    }

    public JobLauncher getJobLauncher(DataSource dataSource) throws Exception {
    	System.out.println("dataSource : "+dataSource);
        SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
        jobLauncher.setJobRepository(getJobRepository(dataSource));
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }
	
}
