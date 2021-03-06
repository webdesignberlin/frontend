define([
    'common/utils/cookies',
    'common/modules/identity/api',
    'common/utils/ajax',
    'common/utils/storage'
], function(
    Cookies,
    Id,
    ajax,
    Storage
) {
    describe('Get user data', function() {
        var config = {
                'page' : {
                    'idApiUrl' : "https://idapi.theguardian.com",
                    'idUrl' : "https://profile.theguardian.com"
                }
            },
            cookieData = 'WyIyMzEwOTU5IiwiamdvcnJpZUBnbWFpbC5jb20iLCJqYW1lc2dvcnJpZSIsIjUzNCIsMTM4Mjk1MzAzMTU5MSwxXQ.MC0CFBsFwIEITO91EGONK4puyO2ZgGQcAhUAqRa7PVDCoAjrbnJNYYvMFec4fAY',
            reqwestReturn = {
                'then': function() {}
            };

        beforeEach(function () {
            sinon.stub(ajax, 'reqwest');
            sinon.stub(reqwestReturn, 'then');
            sinon.stub(Cookies, 'get');
            localStorage.clear();

            ajax.reqwest.returns(reqwestReturn);
            ajax.init(config);

            Id.init(config);
            Id.reset();
        });

        afterEach(function() {
            ajax.reqwest.restore();
            Cookies.get.restore();
            reqwestReturn.then.restore();
        });

        it('gets user from cookie', function() {
            Cookies.get.withArgs("GU_U").returns(cookieData);

            var user = Id.getUserFromCookie();
            expect(user.displayName).toBe('jamesgorrie');
        });

        it('decodes a base64 string', function() {
            var string = 'sammandoo',
                encodedString = window.btoa(string),
                decodedString = Id.decodeBase64(encodedString);

            expect(decodedString).toBe(string);
        });

        it('gets user from the idapi', function() {
            Cookies.get.withArgs("GU_U").returns(cookieData);

            var apiCallback = sinon.spy(),
                expectedUser = {};

            reqwestReturn.then.callsArgWith(
                0,
                {
                    'status' : 'ok',
                    'user' : expectedUser
                }
            );

            Id.getUserFromApi(apiCallback);

            expect(apiCallback.getCall(0).args[0]).toBe(expectedUser);
            expect(ajax.reqwest.getCall(0).args[0]["url"]).toBe("https://idapi.theguardian.com/user/me");
            expect(ajax.reqwest.getCall(0).args[0]["type"]).toBe("jsonp");
            expect(ajax.reqwest.getCall(0).args[0]["crossOrigin"]).toBe(true);
        });

        it('should not call api if the cookie does not exist', function() {
            var apiCallback = sinon.spy();

            Id.getUserFromApi(apiCallback);

            expect(apiCallback.getCall(0).args[0]).toBeNull();
            expect(ajax.reqwest.callCount).toBe(0);
        });

        it('gets user from the idapi', function() {
            Cookies.get.withArgs("GU_U").returns(cookieData);

            var apiCallback = sinon.spy(),
                expectedUser = {};

            reqwestReturn.then.callsArgWith(
                0,
                {
                    'status' : 'ok',
                    'user' : expectedUser
                }
            );

            Id.getUserFromApi(apiCallback);

            expect(apiCallback.getCall(0).args[0]).toBe(expectedUser);
            expect(ajax.reqwest.getCall(0).args[0]["url"]).toBe("https://idapi.theguardian.com/user/me");
            expect(ajax.reqwest.getCall(0).args[0]["type"]).toBe("jsonp");
            expect(ajax.reqwest.getCall(0).args[0]["crossOrigin"]).toBe(true);
        });

        it('should redirect to sign in when user is not signed in', function() {
            var redirectSpy = sinon.stub(Id, 'redirectTo'),
                user = Id.getUserOrSignIn();

            expect(redirectSpy.called).toBeTruthy();
            Id.redirectTo.restore();
        });

        it('should not redirect to sign in when user is already signed in', function() {
            Cookies.get.withArgs("GU_U").returns(cookieData);

            var redirectSpy = sinon.stub(Id, 'redirectTo'),
                user = Id.getUserOrSignIn();

            expect(user.displayName).toBe('jamesgorrie');
            expect(redirectSpy.called).toBeFalsy();
            Id.redirectTo.restore();
        });

        it('should redirect with return URL when given', function() {
            var redirectSpy = sinon.stub(Id, 'redirectTo'),
                returnUrl = 'http://www.theguardian.com/foo',
                user = Id.getUserOrSignIn(returnUrl);

            expect(redirectSpy.getCall(0).args[0]).toContain(encodeURIComponent(returnUrl));
            Id.redirectTo.restore();
        });

        it("should attempt to autosigin an user who is not currently signed in and has not previously signed out", function() {
            Cookies.get.withArgs("GU_U").returns(null);
            Cookies.get.withArgs("GU_SO").returns(null);
            Storage.local.set("gu.id.nextFbCheck", "blah|blah");

            expect(Id.shouldAutoSigninInUser()).toBe(false);
        });

        it("should not attempt to autosigin a user who is not currently signed in, has not previously signed out, before the facebook check overlaps", function() {
            Cookies.get.withArgs("GU_U").returns(null);
            Cookies.get.withArgs("GU_SO").returns(null);

            expect(Id.shouldAutoSigninInUser()).toBe(true);
        });

        it("should not attempt to autosignin a signed in user", function() {
            Cookies.get.withArgs("GU_U").returns(cookieData);

            expect(Id.shouldAutoSigninInUser()).toBe(false);
        });

        it("should attempt to autosignin a user who has signed out more than 24 hours ago after the facebook check has ellapsed", function() {
            var today = new Date(),
                theDayBeforeYesterday = new Date();

            theDayBeforeYesterday.setDate(today.getDate() - 2);

            var timeStampInSeconds = theDayBeforeYesterday.getTime() / 1000;
            Cookies.get.withArgs("GU_SO").returns(timeStampInSeconds.toString);

            expect(Id.shouldAutoSigninInUser()).toBe(true);
        });

        it("should not attempt to autosignin a user who has signed out more than 24 hours ago before the facebook check has ellapsed", function() {
            var theDayBeforeYesterday = new Date();
            theDayBeforeYesterday.setDate(new Date().getDate() - 2);

            var timeStampInSeconds = theDayBeforeYesterday.getTime() / 1000;

            Cookies.get.withArgs("GU_SO").returns(timeStampInSeconds.toString);
            Storage.local.set("gu.id.nextFbCheck", "blah|blah");

            expect(Id.shouldAutoSigninInUser()).toBe(false);
        });

        it("should not attempt to autosignin a user who has signed out within the last 24 hours", function() {
            var fourHoursAgo = new Date();
            fourHoursAgo.setHours(new Date().getHours() - 4);

            var timeStampInSeconds = fourHoursAgo.getTime() / 1000;

            Cookies.get.withArgs("GU_SO").returns(timeStampInSeconds.toString());

            expect(Id.shouldAutoSigninInUser()).toBe(false);
        });
    });
});
