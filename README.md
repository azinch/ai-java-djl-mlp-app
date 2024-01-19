# ai-java-djl-mlp-app

### Project overview

Deep-Java-Library Multi Layer Perceptron app.

This project uses an artificial neural network (ANN) to model Logistic Regression for Telco service usage (a simplified use case). The project is a microservice architecture consisting of:

* **Data Generator Service:** Generates training data with labels indicating overdue payments for Telco service consumption (binary logistic regression).
* **ML Trainer Service:** Trains a deep neural network to model the logistic regression.
* **ML Predictor Service:** Predicts the probability of a user having overdue payments based on provided features.

### Project value

* Demonstrates Deep Learning application for Telco service consumption, covering key Machine Learning aspects like feature engineering, preparation, model selection, training, evaluation, tuning, and prediction.
* Showcases Deep Java Library (DJL) by AWS for designing engine-agnostic Multi Layer Perceptron (MLP) for ANNs, supporting various engines (MXNet, TensorFlow, PyTorch).
* Illustrates Kafka for streaming data for On-Line ML and Oracle for storing trained ML model snapshots.
* Demonstrates containerization and deployment of microservices to OpenShift cluster for monitoring and scaling.
* Serves as an example of using Spring-Kafka integration API, Asynchronous services, Spring JPA, and Spring REST.

### Data generating, model training & predicting flow

**See ml-trainer-predictor-flow.jpg**

1. **Data Generator Service:** Streams Telco service usages (TrainingMessage) to Kafka. (See runDataProducer in MLTrainerService for details)

```
TrainingMessage {
    // customer income in rubles per month
    // service price in rubles per month
    // service usage in minutes per month
    // customer loyalty to company in years
    // customer age
    // customer sex (man/woman)
    // customer marital status (married/single)
    // customer region (north/west/south/east)
    // whether customer pays timely for his service or not (1/0)
}
```

2. **ML Trainer Service:** Streams TrainingMessages from Kafka and trains an MLP model for binary logistic regression using DJL MLP with Apache MXNet engine. Training can be performed on CPU or GPU. (See runTrainer in MLTrainerService for details)

3. **ML Predictor Service:** (a Spring REST web-container) handles POST requests for predicting user payment ability based on given user/service data. It reads the latest trained model from Oracle database and infers the probability of timely payment.

4. **Openshift** manages ML Trainer/Predictor Services as containers. Trainer is scaled vertically (threads, DJL capabilities), Predictor horizontally (multiple pods).

### Devops activities

1. Create 3 Virtual Machines using Windows Hyper-V:
    * Openshift RHEL VM (vCPU=4, RAM=24Gb, SSD=35Gb)
    * Kafka CentOS VM (vCPU=2, RAM=16Gb, SSD=20Gb)
    * OracleXE CentOS VM (vCPU=1, RAM=4Gb)
2. Start Oracle, Kafka, and create topic (see djl-mlp/scripts).
3. Start Openshift CRC, login, create djl-mlp project (namespace).
4. Build & deploy djl-mlp microservices to Openshift:
    * **Trainer mS:**
        * `cd djl-mlp`
        * Set `runTrainer=Yes` in `application.properties`
        * `gradle clean build`
        * `deploy-app-to-openshift trainer`
    * **Predictor mS:**
        * Set `runTrainer=No` in `application.properties`
        * `gradle clean build`
        * `deploy-app-to-openshift predictor 9097 3`

### Technology stack

1. DJL AWS
2. Confluent Kafka
3. Oracle XE
4. Openshift CRC
5. Google Postman
6. Spring Boot, Spring Kafka, REST, JPA (Oracle), Gradle
7. RHEL/CentOS VMs

