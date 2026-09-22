package com.campus.utils;

import com.campus.dto.StudentDTO;

public class StudentHolder {
    private static final ThreadLocal<StudentDTO> tl = new ThreadLocal<>();

    public static void saveStudent(StudentDTO student) {
        tl.set(student);
    }

    public static StudentDTO getStudent() {
        return tl.get();
    }

    public static Long getStudentId() {
        StudentDTO student = tl.get();
        return student == null ? null : student.getId();
    }

    public static void removeStudent() {
        tl.remove();
    }
}
