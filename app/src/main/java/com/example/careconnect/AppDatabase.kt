package com.example.careconnect

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Medicine::class,
        User::class,
        Appointment::class,
        MedicalRecord::class,
        Prescription::class
    ],
    version = 3
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun userDao(): UserDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun medicalRecordDao(): MedicalRecordDao
    abstract fun prescriptionDao(): PrescriptionDao
}
