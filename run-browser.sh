#!/bin/bash

# Modern Java Browser Launcher Script
# یہ script آپ کے Java browser کو آسانی سے run کرنے کے لیے ہے

echo "🚀 Starting Modern Java Browser..."
echo "📖 Starting کر رہے ہیں Modern Java Browser..."

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven is not installed. Please install Maven first."
    echo "❌ Maven install نہیں ہے۔ پہلے Maven install کریں۔"
    exit 1
fi

# Check if Java 17+ is available
if ! command -v java &> /dev/null; then
    echo "❌ Java is not installed. Please install Java 17 or higher."
    echo "❌ Java install نہیں ہے۔ Java 17 یا اس سے اوپر install کریں۔"
    exit 1
fi

# Display Java version
echo "☕ Java Version:"
java -version

echo ""
echo "🔧 Building the application..."
echo "🔧 Application build کر رہے ہیں..."

# Clean and compile
mvn clean compile

if [ $? -ne 0 ]; then
    echo "❌ Build failed. Please check the errors above."
    echo "❌ Build fail ہو گیا۔ اوپر کی errors چیک کریں۔"
    exit 1
fi

echo ""
echo "✅ Build successful! Starting the browser..."
echo "✅ Build کامیاب! Browser start کر رہے ہیں..."

# Run the application
mvn javafx:run

echo ""
echo "👋 Browser session ended."
echo "👋 Browser session ختم ہو گیا۔"