/**
 * BharatConnect Client Canonical Phone Normalization Utility (www/utils/phone.js)
 */

window.BharatConnectPhone = {
    /**
     * Normalizes input phone string into a clean 10-digit national canonical string.
     * Strips non-digits, country code prefixes (+91, 91), and leading zeros.
     * 
     * @param {string} phone
     * @param {string} defaultCountryCode
     * @returns {string} 10-digit canonical phone string
     */
    normalizePhone: function(phone, defaultCountryCode = '91') {
        if (!phone) return '';
        let digits = String(phone).replace(/\D/g, '');
        if (!digits) return '';

        digits = digits.replace(/^0+/, '');

        if (digits.length === 12 && digits.startsWith('91')) {
            digits = digits.slice(2);
        } else if (digits.length > 10 && digits.startsWith(defaultCountryCode)) {
            digits = digits.slice(defaultCountryCode.length);
        }

        if (digits.length > 10) {
            digits = digits.slice(-10);
        }

        return digits;
    },

    /**
     * Formats phone number to E.164 string format (e.g., '+919876543210')
     * @param {string} phone 
     * @param {string} countryCode 
     * @returns {string}
     */
    toE164: function(phone, countryCode = '+91') {
        const norm = this.normalizePhone(phone);
        if (!norm) return '';
        const prefix = countryCode.startsWith('+') ? countryCode : '+' + countryCode;
        return prefix + norm;
    },

    /**
     * Validates whether string is a valid 10-digit Indian mobile number
     * @param {string} phone 
     * @returns {boolean}
     */
    validatePhone: function(phone) {
        const norm = this.normalizePhone(phone);
        return norm.length === 10 && ['6', '7', '8', '9'].includes(norm[0]);
    }
};
