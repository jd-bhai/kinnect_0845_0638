<?php
require_once 'db.php';

$uploadDir = __DIR__ . '/uploads/';
if (!file_exists($uploadDir)) {
    mkdir($uploadDir, 0777, true);
}

$response = ["status" => "error", "message" => "No file uploaded"];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    if (isset($_FILES['file'])) {
        $fileName = time() . '_' . basename($_FILES['file']['name']);
        $targetFile = $uploadDir . $fileName;

        if (move_uploaded_file($_FILES['file']['tmp_name'], $targetFile)) {
            $protocol = isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http";
            $fileUrl = $protocol . "://" . $_SERVER['HTTP_HOST'] . "/uploads/" . $fileName;
            $response = ["status" => "success", "url" => $fileUrl, "filename" => $fileName];
        } else {
            $response = ["status" => "error", "message" => "Failed to save file to server filesystem"];
        }
    } else if (isset($_POST['base64_image'])) {
        $base64Data = $_POST['base64_image'];
        $cleanData = preg_replace('#^data:image/\w+;base64,#i', '', $base64Data);
        $decodedData = base64_decode($cleanData);

        $fileName = time() . '_' . uniqid() . '.jpg';
        $targetFile = $uploadDir . $fileName;

        if (file_put_contents($targetFile, $decodedData)) {
            $protocol = isset($_SERVER['HTTPS']) && $_SERVER['HTTPS'] === 'on' ? "https" : "http";
            $fileUrl = $protocol . "://" . $_SERVER['HTTP_HOST'] . "/uploads/" . $fileName;
            $response = ["status" => "success", "url" => $fileUrl, "filename" => $fileName];
        } else {
            $response = ["status" => "error", "message" => "Failed to save base64 image"];
        }
    }
}

echo json_encode($response);
?>