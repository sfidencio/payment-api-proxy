# 1. Clean and build the project generating the JAR
mvn clean package -DskipTests

# 2. Check if the JAR was created successfully
if [ $? -ne 0 ]; then
    echo "Error compiling the project. Check the logs."
    exit 1
fi

# 3. Log in to Docker Hub (replace USERNAME with your user)
docker login -u sfidencio

# 4. Check if the login was successful
if [ $? -ne 0 ]; then
    echo "Error logging in to Docker Hub. Check your credentials."
    exit 1
fi

# 5. Build the Docker image (replace IMAGE_NAME with the desired name)
docker build -t sfidencio/payment-api-proxy:latest .

# 6. Check if the image was created successfully
if [ $? -ne 0 ]; then
    echo "Error building the Docker image. Check the Dockerfile."
    exit 1
fi

# 7. Push the image to Docker Hub
docker push sfidencio/payment-api-proxy:latest
# 8. Check if the push was successful
if [ $? -ne 0 ]; then
    echo "Error pushing the image to Docker Hub. Check your connection."
    exit 1
fi
echo "Docker image successfully created and pushed to Docker Hub."