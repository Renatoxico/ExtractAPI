# Use an official Node.js runtime as a parent image
FROM node:18

# Set the working directory in the container
WORKDIR /app

# Copy package.json and package-lock.json into the container
COPY package*.json ./

# Remove any existing node_modules and package-lock.json to avoid conflicts
RUN npm install 

# Copy the rest of the application code into the container
COPY . .

# Build the application
# RUN npm run build

# Expose the port the app runs on
EXPOSE 5173

# Start the application
CMD ["npm", "run", "dev", "--", "--host"]