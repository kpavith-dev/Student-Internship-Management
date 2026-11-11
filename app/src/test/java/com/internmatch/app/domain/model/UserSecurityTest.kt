package com.internmatch.app.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserSecurityTest {

    @Test
    fun userRole_extensionProperties_correctlyEvaluateRoles() {
        val student = User("1", "s@test.com", "Student", UserRole.STUDENT)
        val provider = User("2", "p@test.com", "Provider", UserRole.PROVIDER)
        val admin = User("3", "a@test.com", "Admin", UserRole.ADMIN)

        assertTrue(student.isStudent)
        assertFalse(student.isProvider)
        assertFalse(student.isAdmin)

        assertTrue(provider.isProvider)
        assertFalse(provider.isStudent)
        assertFalse(provider.isAdmin)

        assertTrue(admin.isAdmin)
        assertFalse(admin.isStudent)
        assertFalse(admin.isProvider)

        assertTrue(UserRole.PROVIDER.canAccessProviderPortal())
        assertTrue(UserRole.ADMIN.canAccessProviderPortal())
        assertFalse(UserRole.STUDENT.canAccessProviderPortal())

        assertTrue(UserRole.ADMIN.canAccessAdminConsole())
        assertFalse(UserRole.STUDENT.canAccessAdminConsole())
        assertFalse(UserRole.PROVIDER.canAccessAdminConsole())
    }
}
