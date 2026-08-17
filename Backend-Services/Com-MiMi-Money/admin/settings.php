<?php
/**
 * Created by PhpStorm.
 * User: abderrahimelimame
 * Date: 8/10/16
 * Time: 03:23
 */
include 'header.php';
if ($_GB->getSession('admin') == false) {
    header("location:login.php");
}
$moneyBannerUnitID = $_GB->getSettings('money_banner_ads_unit_id');
$moneyBannerStatus = $_GB->getSettings('money_banner_ads_status');
?>

    <div class="box bg-gray-light ">
        <center>
            <div class="box-body ">
                <form role="form" action="" method="POST">
                    <div class="callout callout-info bg-blue-gradient">
                        <h4>General Settings</h4>
                    </div>
                    <div class="form-group">
                        <label for="privacy_policy">Privacy Policy</label>
                    <textarea class="form-control" rows="10" cols="50" name="privacy_policy" id="privacy_policy">
                        <?php echo htmlentities($_GB->getSettings('privacy_policy')); ?>
                    </textarea>

                    </div>

                    <div class="form-group">
                        <label for="app_version">Application Version Code Ex: 26</label>
                        <input class="form-control" type="text"
                               value="<?php echo $_GB->getSettings('app_version'); ?>"
                               name="app_version" id="app_version">

                    </div>
                    <div class="form-group">
                        <label for="app_name">Application Name Ex:WhatsClone</label>
                        <input class="form-control" type="text" value="<?php echo $_GB->getSettings('app_name'); ?>"
                               name="app_name" id="app_name" required>

                    </div>
                    <div class="callout callout-info bg-blue-gradient">
                        <h4>Admob Settings "Banner ads"</h4>
                    </div>
                    <div class="form-group">
                        <label for="admob_banner_ads_unit_id">Admob Banner Unit ID</label>
                        <input class="form-control" type="text"
                               value="<?php echo $_GB->getSettings('banner_ads_unit_id'); ?>"
                               name="admob_banner_ads_unit_id" id="admob_banner_ads_unit_id">

                    </div>
                    <div class="form-group">
                        <label for="admob_banner_ads_status">
                            <span>Enable Banner Ads</span>
                            <?php $status = $_GB->getSettings('banner_ads_status');
                            if ($status == 1) {
                                echo '<input type="checkbox" name="admob_banner_ads_status" id="admob_banner_ads_status" class="flat-red"  checked>';
                            } else {
                                echo '<input type="checkbox" name="admob_banner_ads_status" id="admob_banner_ads_status" class="flat-red"  >';
                            } ?>

                        </label>
                    </div>
                    <div class="callout callout-info bg-blue-gradient">
                        <h4>Admob Settings "Interstitial ads"</h4>
                    </div>
                    <div class="form-group">
                        <label for="admob_interstitial_unit_id">Admob Interstitial Unit ID</label>
                        <input class="form-control" type="text"
                               value="<?php echo $_GB->getSettings('interstitial_ads_unit_id'); ?>"
                               name="admob_interstitial_unit_id" id="admob_interstitial_unit_id">
                    </div>
                    <div class="form-group">
                        <label for="admob_interstitial_ads_status">
                            <span>Enable Interstitial Ads</span>
                            <?php $status = $_GB->getSettings('interstitial_ads_status');
                            if ($status == 1) {
                                echo '<input type="checkbox" name="admob_interstitial_ads_status" id="admob_interstitial_ads_status" class="flat-red" checked>';
                            } else {
                                echo '<input type="checkbox" name="admob_interstitial_ads_status" id="admob_interstitial_ads_status" class="flat-red" >';
                            } ?>

                        </label>

                    </div>
                    <div class="callout callout-info bg-blue-gradient">
                        <h4>Admob Settings "Wallet Banner ads"</h4>
                    </div>
                    <div class="form-group">
                        <label for="admob_wallet_banner_ads_unit_id">Admob Wallet Banner Unit ID</label>
                        <input class="form-control" type="text"
                               value="<?php echo $_GB->getSettings('wallet_banner_ads_unit_id'); ?>"
                               name="admob_wallet_banner_ads_unit_id" id="admob_wallet_banner_ads_unit_id">

                    </div>
                    <div class="form-group">
                        <label for="admob_wallet_banner_ads_status">
                            <span>Enable Wallet Banner Ads</span>
                            <?php $status = $_GB->getSettings('wallet_banner_ads_status');
                            if ($status == 1) {
                                echo '<input type="checkbox" name="admob_wallet_banner_ads_status" id="admob_wallet_banner_ads_status" class="flat-red"  checked>';
                            } else {
                                echo '<input type="checkbox" name="admob_wallet_banner_ads_status" id="admob_wallet_banner_ads_status" class="flat-red"  >';
                            } ?>

                        </label>
                    </div>
                    <div class="callout callout-info bg-blue-gradient">
                        <h4>Admob Settings "Money Banner ads"</h4>
                    </div>
                    <div class="form-group">
                        <label for="admob_money_banner_ads_unit_id">Admob Money Banner Unit ID</label>
                        <input class="form-control" type="text"
                               value="<?php echo $moneyBannerUnitID; ?>"
                               name="admob_money_banner_ads_unit_id" id="admob_money_banner_ads_unit_id">

                    </div>
                    <div class="form-group">
                        <label for="admob_money_banner_ads_status">
                            <span>Enable Money Banner Ads</span>
                            <?php $status = $moneyBannerStatus;
                            if ($status == 1) {
                                echo '<input type="checkbox" name="admob_money_banner_ads_status" id="admob_money_banner_ads_status" class="flat-red"  checked>';
                            } else {
                                echo '<input type="checkbox" name="admob_money_banner_ads_status" id="admob_money_banner_ads_status" class="flat-red"  >';
                            } ?>

                        </label>
                    </div>
                    <div class="callout callout-info bg-blue-gradient">
                        <h4>Admob Settings "Space Banner ads"</h4>
                    </div>
                    <div class="form-group">
                        <label for="admob_space_banner_ads_unit_id">Admob Space Banner Unit ID</label>
                        <input class="form-control" type="text"
                               value="<?php echo $_GB->getSettings('space_banner_ads_unit_id'); ?>"
                               name="admob_space_banner_ads_unit_id" id="admob_space_banner_ads_unit_id">

                    </div>
                    <div class="form-group">
                        <label for="admob_space_banner_ads_status">
                            <span>Enable Space Banner Ads</span>
                            <?php $status = $_GB->getSettings('space_banner_ads_status');
                            if ($status == 1) {
                                echo '<input type="checkbox" name="admob_space_banner_ads_status" id="admob_space_banner_ads_status" class="flat-red"  checked>';
                            } else {
                                echo '<input type="checkbox" name="admob_space_banner_ads_status" id="admob_space_banner_ads_status" class="flat-red"  >';
                            } ?>

                        </label>
                    </div>
                    <div class="callout callout-info bg-blue-gradient">
                        <h4>Admob Settings "Video ads"</h4>
                    </div>
                    <div class="form-group">
                        <label for="admob_video_unit_id">Admob Video Unit ID</label>
                        <input class="form-control" type="text"
                               value="<?php echo $_GB->getSettings('video_ads_unit_id'); ?>"
                               name="admob_video_unit_id" id="admob_video_unit_id">

                        <label for="admob_video_app_id">Admob Video APP ID</label>
                        <input class="form-control" type="text"
                               value="<?php echo $_GB->getSettings('video_ads_app_id'); ?>"
                               name="admob_video_app_id" id="admob_video_app_id">
                    </div>
                    <div class="form-group">
                        <label for="admob_video_ads_status">
                            <span>Enable Video Ads</span>
                            <?php $status = $_GB->getSettings('video_ads_status');
                            if ($status == 1) {
                                echo '<input type="checkbox" name="admob_video_ads_status" id="admob_video_ads_status" class="flat-red" checked>';
                            } else {
                                echo '<input type="checkbox" name="admob_video_ads_status" id="admob_video_ads_status" class="flat-red" >';
                            } ?>

                        </label>

                    </div>

                    <div class="callout callout-info bg-blue-gradient">
                        <h4>TURN Server Settings</h4>
                    </div>
                    <div class="form-group">
                        <label for="meteredTurnApiKey">Metered TURN API Key</label>
                        <input class="form-control" type="text"
                               value="<?php echo $_GB->getSettings('meteredTurnApiKey'); ?>"
                               name="meteredTurnApiKey" id="meteredTurnApiKey">
                    </div>
                    <div class="form-group">
                        <label for="turnServerUrl">Custom TURN Server URL (e.g. turn:turn.example.com:3478)</label>
                        <input class="form-control" type="text"
                               value="<?php echo $_GB->getSettings('turnServerUrl'); ?>"
                               name="turnServerUrl" id="turnServerUrl">
                    </div>
                    <div class="form-group">
                        <label for="turnServerUsername">Custom TURN Username</label>
                        <input class="form-control" type="text"
                               value="<?php echo $_GB->getSettings('turnServerUsername'); ?>"
                               name="turnServerUsername" id="turnServerUsername">
                    </div>
                    <div class="form-group">
                        <label for="turnServerCredential">Custom TURN Credential</label>
                        <input class="form-control" type="password"
                               value="<?php echo $_GB->getSettings('turnServerCredential'); ?>"
                               name="turnServerCredential" id="turnServerCredential">
                    </div>

                    <div class="callout callout-info bg-blue-gradient">
                        <h4>Firebase settings</h4>
                    </div>

                    <div class="form-group">
                        <label for="googleApiKey"> google Api Key</label>
                        <input class="form-control" type="text"
                               value="<?php echo $_GB->getSettings('googleApiKey'); ?>"
                               name="googleApiKey" id="googleApiKey" required>
                    </div>

                    <div class="form-group">
                        <label for="googleSenderId">google Sender Id</label>
                        <input class="form-control" type="text"
                               value="<?php echo $_GB->getSettings('googleSenderId'); ?>"
                               name="googleSenderId" id="googleSenderId" required>
                    </div>

                    <div class="form-group">
                        <label for="firebase_project_id">Firebase Project ID (for FCM HTTP v1 API)</label>
                        <input class="form-control" type="text"
                               value="<?php echo $_GB->getSettings('firebase_project_id'); ?>"
                               name="firebase_project_id" id="firebase_project_id">
                    </div>

                    <div class="form-group"></div>
                    <button type="submit"
                            class="btn  btn-success btn-lg">
                        <i>Save Changes</i></button>


                </form>
            </div>
        </center>
    </div>


<?php
if (isset($_POST['privacy_policy']) ||
    isset($_POST['app_name']) ||
    isset($_POST['googleApiKey']) ||
    isset($_POST['googleSenderId']) ||
    isset($_POST['firebase_project_id']) ||
    isset($_POST['app_version']) ||
    isset($_POST['admob_banner_ads_unit_id']) ||
    isset($_POST['admob_banner_ads_status']) ||
    isset($_POST['admob_interstitial_unit_id']) ||
    isset($_POST['admob_interstitial_ads_status']) ||
    isset($_POST['admob_video_unit_id']) ||
    isset($_POST['admob_video_app_id']) ||
    isset($_POST['admob_video_ads_status']) ||
    isset($_POST['admob_wallet_banner_ads_unit_id']) ||
    isset($_POST['admob_wallet_banner_ads_status']) ||
    isset($_POST['admob_money_banner_ads_unit_id']) ||
    isset($_POST['admob_money_banner_ads_status']) ||
    isset($_POST['admob_space_banner_ads_unit_id']) ||
    isset($_POST['admob_space_banner_ads_status']) ||
    isset($_POST['meteredTurnApiKey']) ||
    isset($_POST['turnServerUrl']) ||
    isset($_POST['turnServerUsername']) ||
    isset($_POST['turnServerCredential'])
) {
    $privacy_policy = $_POST['privacy_policy'];
    $app_name = $_POST['app_name'];
    $googleApiKey = $_POST['googleApiKey'];
    $googleSenderId = $_POST['googleSenderId'];
    $firebase_project_id = isset($_POST['firebase_project_id']) ? $_POST['firebase_project_id'] : '';
    $app_version = $_POST['app_version'];

    $admob_banner_ads_unit_id = $_POST['admob_banner_ads_unit_id'];
    $admob_banner_ads_status = isset($_POST['admob_banner_ads_status']) ? "1" : "0";
    $admob_interstitial_unit_id = $_POST['admob_interstitial_unit_id'];
    $admob_interstitial_ads_status = isset($_POST['admob_interstitial_ads_status']) ? "1" : "0";

    $admob_video_unit_id = $_POST['admob_video_unit_id'];
    $admob_video_app_id = $_POST['admob_video_app_id'];
    $admob_video_ads_status = isset($_POST['admob_video_ads_status']) ? "1" : "0";

    $admob_wallet_banner_ads_unit_id = $_POST['admob_wallet_banner_ads_unit_id'];
    $admob_wallet_banner_ads_status = isset($_POST['admob_wallet_banner_ads_status']) ? "1" : "0";
    $admob_money_banner_ads_unit_id = $_POST['admob_money_banner_ads_unit_id'];
    $admob_money_banner_ads_status = isset($_POST['admob_money_banner_ads_status']) ? "1" : "0";
    $admob_space_banner_ads_unit_id = $_POST['admob_space_banner_ads_unit_id'];
    $admob_space_banner_ads_status = isset($_POST['admob_space_banner_ads_status']) ? "1" : "0";

    $meteredTurnApiKey = isset($_POST['meteredTurnApiKey']) ? $_POST['meteredTurnApiKey'] : '';
    $turnServerUrl = isset($_POST['turnServerUrl']) ? $_POST['turnServerUrl'] : '';
    $turnServerUsername = isset($_POST['turnServerUsername']) ? $_POST['turnServerUsername'] : '';
    $turnServerCredential = isset($_POST['turnServerCredential']) ? $_POST['turnServerCredential'] : '';


    $_GB->updateSettings("privacy_policy", $privacy_policy);
    $_GB->updateSettings("app_name", $app_name);
    $_GB->updateSettings("app_version", $app_version);
    $_GB->updateSettings("banner_ads_unit_id", $admob_banner_ads_unit_id);
    $_GB->updateSettings("banner_ads_status", $admob_banner_ads_status);
    $_GB->updateSettings("interstitial_ads_unit_id", $admob_interstitial_unit_id);
    $_GB->updateSettings("interstitial_ads_status", $admob_interstitial_ads_status);

    $_GB->updateSettings("video_ads_unit_id", $admob_video_unit_id);
    $_GB->updateSettings("video_ads_status", $admob_video_ads_status);
    $_GB->updateSettings("video_ads_app_id", $admob_video_app_id);

    $_GB->updateSettings("wallet_banner_ads_unit_id", $admob_wallet_banner_ads_unit_id);
    $_GB->updateSettings("wallet_banner_ads_status", $admob_wallet_banner_ads_status);
    $_GB->updateSettings("money_banner_ads_unit_id", $admob_money_banner_ads_unit_id);
    $_GB->updateSettings("money_banner_ads_status", $admob_money_banner_ads_status);
    $_GB->updateSettings("space_banner_ads_unit_id", $admob_space_banner_ads_unit_id);
    $_GB->updateSettings("space_banner_ads_status", $admob_space_banner_ads_status);

    $_GB->updateSettings("googleApiKey", $googleApiKey);
    $_GB->updateSettings("googleSenderId", $googleSenderId);
    $_GB->updateSettings("firebase_project_id", $firebase_project_id);

    $_GB->updateSettings("meteredTurnApiKey", $meteredTurnApiKey);
    $_GB->updateSettings("turnServerUrl", $turnServerUrl);
    $_GB->updateSettings("turnServerUsername", $turnServerUsername);
    $_GB->updateSettings("turnServerCredential", $turnServerCredential);


    echo $_GB->ErrorDisplay('Settings updated successfully', 'yes');
    header("Refresh: 1; url=settings.php");

}

include 'footer.php';
?>