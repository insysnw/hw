from snmpserv import integer, octet_string, null, object_identifier


def my_response(oid):
    res = '|'.join(oid.split('.'))
    return octet_string('response: {}'.format(res))


DATA = {
    '1.3.6.1.4.1.1.1.0': integer(6666),
    '1.3.6.1.4.1.1.6.0': integer(1234),
    '1.3.6.1.4.1.1.1.1': integer(100),
    '1.3.6.1.4.1.1.3.0': octet_string('Test string'),
    '1.3.6.1.4.1.1.2.0': octet_string('Lab2 server'),
    '1.3.6.1.4.1.1.4.0': null(),
    '1.3.6.1.4.1.1.5.0': object_identifier('1.3.6.7.8.9'),
}