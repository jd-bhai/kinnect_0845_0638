<?php
header("Access-Control-Allow-Origin: *");
header("Content-Type: application/json; charset=UTF-8");

$host = 'localhost';
$db   = 'kinnect_db';
$user = 'kinnect_user';
$pass = 'kinnect_pass';
$charset = 'utf8mb4';

$dsn = "mysql:host=$host;dbname=$db;charset=$charset";
$options = [
    PDO::ATTR_ERRMODE            => PDO::ERRMODE_EXCEPTION,
    PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
    PDO::ATTR_EMULATE_PREPARES   => false,
];

try {
    $pdo = new PDO($dsn, $user, $pass, $options);
} catch (\PDOException $e) {
    // Fallback try root with no password if kinnect_user is not created
    try {
        $pdo = new PDO("mysql:host=$host;dbname=$db;charset=$charset", "root", "", $options);
    } catch (\PDOException $ex) {
        echo json_encode(["status" => "error", "message" => "Database connection failed: " . $ex->getMessage()]);
        exit;
    }
}
?>