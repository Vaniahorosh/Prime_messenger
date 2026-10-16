package com.messenger.prime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidationUtilsTest {

    @Test
    fun testIsValidLoginFormat_validLogins() {
        assertTrue(ValidationUtils.isValidLoginFormat("john_doe"))
        assertTrue(ValidationUtils.isValidLoginFormat("user123"))
        assertTrue(ValidationUtils.isValidLoginFormat("Developer_2026"))
        assertTrue(ValidationUtils.isValidLoginFormat("A_B_C"))
    }

    @Test
    fun testIsValidLoginFormat_invalidLogins() {
        assertFalse(ValidationUtils.isValidLoginFormat("john doe")) // contains space
        assertFalse(ValidationUtils.isValidLoginFormat("user-123")) // contains hyphen
        assertFalse(ValidationUtils.isValidLoginFormat("пользователь")) // cyrillic
        assertFalse(ValidationUtils.isValidLoginFormat("user@mail.com")) // special chars
    }

    @Test
    fun testValidateLogin_lengthAndFormat() {
        // Empty
        val emptyErr = ValidationUtils.validateLogin("")
        assertTrue(emptyErr?.contains("не может быть пустым") == true)

        // Too short (< 3)
        val shortErr = ValidationUtils.validateLogin("ab")
        assertTrue(shortErr?.contains("Слишком короткий") == true)

        // Too long (> 25)
        val longErr = ValidationUtils.validateLogin("a".repeat(26))
        assertTrue(longErr?.contains("Слишком длинный") == true)

        // Invalid characters
        val charErr = ValidationUtils.validateLogin("invalid-user")
        assertTrue(charErr?.contains("только латинские буквы") == true)

        // Valid login
        val validResult = ValidationUtils.validateLogin("valid_user1")
        assertNull(validResult)
    }

    @Test
    fun testValidateLogin_restrictedKeywords() {
        val primeErr = ValidationUtils.validateLogin("prime")
        assertTrue(primeErr?.contains("зарезервирован") == true)

        val adminErr = ValidationUtils.validateLogin("admin")
        assertTrue(adminErr?.contains("зарезервирован") == true)

        val supportErr = ValidationUtils.validateLogin("support")
        assertTrue(supportErr?.contains("зарезервирован") == true)
    }

    @Test
    fun testIsRestricted_homoglyphsAndObfuscation() {
        assertTrue(ValidationUtils.isRestricted("prime"))
        assertTrue(ValidationUtils.isRestricted("pr!me"))
        assertTrue(ValidationUtils.isRestricted("pr1me"))
        assertTrue(ValidationUtils.isRestricted("PRIME"))
        assertTrue(ValidationUtils.isRestricted("admin"))
        assertTrue(ValidationUtils.isRestricted("adm!n"))
        assertTrue(ValidationUtils.isRestricted("@dm!n"))
        assertTrue(ValidationUtils.isRestricted("админ"))
        assertTrue(ValidationUtils.isRestricted("службаподдержки"))
        assertTrue(ValidationUtils.isRestricted("служба_поддержки"))
        assertFalse(ValidationUtils.isRestricted("alex_smith"))
    }

    @Test
    fun testValidateName_validAndInvalid() {
        // Empty name
        val emptyErr = ValidationUtils.validateName("  ")
        assertTrue(emptyErr?.contains("не может быть пустым") == true)

        // Too short
        val shortErr = ValidationUtils.validateName("A")
        assertTrue(shortErr?.contains("минимум 2 символа") == true)

        // Too long
        val longErr = ValidationUtils.validateName("A".repeat(33))
        assertTrue(longErr?.contains("слишком длинное") == true)

        // Valid name
        val validErr = ValidationUtils.validateName("Алексей")
        assertNull(validErr)
    }

    @Test
    fun testValidatePassword_validAndInvalid() {
        // Empty password
        val emptyErr = ValidationUtils.validatePassword("")
        assertTrue(emptyErr?.contains("не может быть пустым") == true)

        // Too short (< 8)
        val shortErr = ValidationUtils.validatePassword("1234567")
        assertTrue(shortErr?.contains("слишком короткий") == true)

        // Too long (> 32)
        val longErr = ValidationUtils.validatePassword("P".repeat(33))
        assertTrue(longErr?.contains("слишком длинный") == true)

        // Valid password
        val validErr = ValidationUtils.validatePassword("SecretPass123!")
        assertNull(validErr)
    }
}
