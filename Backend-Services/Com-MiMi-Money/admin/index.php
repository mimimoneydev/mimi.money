<?php
/**
 * Created by PhpStorm.
 * User: abderrahimelimame
 * Date: 7/8/16
 * Time: 02:00
 */
include 'header.php';
if ($_GB->getSession('admin') == false) {
    header("location:login.php");
}
?>
<div class="box bg-gray-light "></div>
<!-- Main content -->
<div class="content">

    <!-- Your Page Content Here -->
    <?php
    $totalUsers = $_DB->CountRows('users');
    $totalGroups = $_DB->CountRows('groups');
    $totalMessages = $_DB->CountRows('messages');
    $totalCalls = $_DB->CountRows('calls'); ?>

    <div class="row center-block">
        <div class="col-md-3">
            <!-- small box -->
            <div class="small-box bg-blue-gradient">
                <div class="inner">
                    <h3><?php echo $totalMessages ?></h3>
                    <p>Messages</p>
                </div>
                <a href="messages.php?cmd=messages" class="small-box-footer">More info <i
                        class="fa fa-arrow-circle-right"></i></a>
            </div>
        </div>
        <!-- ./col -->
        <div class="col-md-3">
            <!-- small box -->
            <div class="small-box bg-orange-active">
                <div class="inner">
                    <h3><?php echo $totalUsers ?></h3>
                    <p>Users</p>
                </div>
                <a href="users.php" class="small-box-footer">More info <i class="fa fa-arrow-circle-right"></i></a>
            </div>
        </div>

        <div class="col-md-3">
            <!-- small box -->
            <div class="small-box bg-maroon-gradient">
                <div class="inner">
                    <h3><?php echo $totalGroups ?></h3>

                    <p>Groups</p>
                </div>
                <a href="groups.php?cmd=groups" class="small-box-footer">More info <i
                        class="fa fa-arrow-circle-right"></i></a>
            </div>
        </div>
        <div class="col-md-3">
            <!-- small box -->
            <div class="small-box bg-red-gradient">
                <div class="inner">
                    <h3><?php echo $totalCalls ?></h3>

                    <p>Calls</p>
                </div>
                <a href="calls.php?cmd=calls" class="small-box-footer">More info</a>
            </div>
        </div>

    </div>

    <!-- USERS LIST -->
    <div class="box box-danger">
        <div class="box-header with-border">
            <h3 class="box-title">Latest Members</h3>

            <div class="box-tools pull-right">
                <button type="button" class="btn btn-box-tool" data-widget="collapse"><i
                        class="fa fa-minus"></i>
                </button>
                <button type="button" class="btn btn-box-tool" data-widget="remove"><i class="fa fa-times"></i>
                </button>
            </div>
        </div>
        <!-- /.box-header -->
        <div class="box-body no-padding">
            <ul class="users-list clearfix">

                <?php
                $query = $_DB->select('users', '*', '', '`id` DESC', 8);
                while ($fetch = $_DB->fetchAssoc($query)) {
                    $username = $fetch['username'];
                    $userImage = $fetch['image'];
                    echo '<li>';

                    echo '<div class="widget-user-image">';
                    if ($userImage != null) { ?>
                        <img class="img-circle" alt="User Avatar"
                             src="../image/settings/<?php echo $userImage ?>" style="width: 100px; height: 100px"
                             onerror="this.src='image_holder_ur_circle.png'">
                    <?php } else { ?>
                        <img class="img-circle" alt="User Avatar" style="width: 100px; height: 100px"
                             src="image_holder_ur_circle.png">
                        <?php
                    }

                    echo '</div>';
                    echo '<a class="users-list-name" >';
                    if ($username == null) {
                        echo $fetch['wallet_address'];
                    } else {
                        echo $fetch['username'];
                    }
                    echo '</a>';
                    echo '<span class="users-list-date">';
                    echo $fetch['status'];
                    echo '</span>';
                    echo '</li>';
                } ?>
            </ul>
            <!-- /.users-list -->
        </div>
        <!-- /.box-body -->
        <div class="box-footer text-center">
            <a href="users.php?cmd=users" class="uppercase">View All Users</a>
        </div>
        <!-- /.box-footer -->
    </div>
    <div class="row">

        <div class="col-md-6">
            <!-- Map box -->
            <div class="box box-solid bg-aqua">
                <div class="box-header">
                    <!-- tools box -->
                    <div class="pull-right box-tools">
                        <button type="button" class="btn btn-primary btn-sm pull-right" data-widget="collapse"
                                data-toggle="tooltip" title="Collapse" style="margin-right: 5px;">
                            <i class="fa fa-minus"></i></button>
                    </div>
                    <!-- /. tools -->

                    <i class="fa fa-map-marker"></i>

                    <h3 class="box-title">
                        Users registration by country
                    </h3>
                </div>
                <div class="box-body bg-aqua">
                    <div id="regions_div" style="height: 300px; width: 100%;"></div>
                </div>
            </div>
        </div>

        <div class="col-md-6">

            <!-- /.info-box -->

            <div class="box box-default">
                <div class="box-header with-border">
                    <h3 class="box-title">Browser Usage</h3>

                    <div class="box-tools pull-right">
                        <button type="button" class="btn btn-box-tool" data-widget="collapse"><i
                                class="fa fa-minus"></i>
                        </button>
                        <button type="button" class="btn btn-box-tool" data-widget="remove"><i class="fa fa-times"></i>
                        </button>
                    </div>
                </div>
                <!-- /.box-header -->
                <div class="box-body">
                    <div class="row">
                        <div class="col-md-8">
                            <div class="chart-responsive">
                                <div id="pieChart"></div>
                            </div>
                            <!-- ./chart-responsive -->
                        </div>

                    </div>
                    <!-- /.row -->
                </div>
                <!-- /.box-body -->
            </div>
        </div>
    </div>
</div>
<!-- /.content -->

<script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
<script type="text/javascript" src="https://www.google.com/jsapi"></script>

<?php
$query = $_DB->selectDistinct('users', 'country', '', '`country` DESC');
$countries = array();
$userNumber = array();

while ($fetch = $_DB->fetchAssoc($query)) {
    $fetch['country'] = (empty($fetch['country'])) ? null : $fetch['country'];
    $fetch['userCounter'] = $_DB->CountRows('users', "`country`= '{$fetch['country']}'");
    array_push($countries, $fetch['country']);
    array_push($userNumber, $fetch['userCounter']);
}
$countriesData = array(['Country', 'Popularity']);
foreach ($countries as $k => $v) {
    $countriesData[] = array($v, $userNumber[$k]);
}
?>
<script>
    /*for google charts */

    google.charts.load('current', {'packages': ['geochart', 'corechart']});
    google.charts.setOnLoadCallback(drawRegionsMap);

    function drawRegionsMap() {

        var data = google.visualization.arrayToDataTable(<?php echo json_encode($countriesData)?>);

        var options = {
            colorAxis: {
                colors: ['#00c0ef', '#dd4b39', '#0073b7'],
                minValue: 0,
                maxValue: 2
            }, backgroundColor: {fill: '#00c0ef'}
        };

        var chart = new google.visualization.GeoChart(document.getElementById('regions_div'));

        chart.draw(data, options);

        function resizeHandler() {
            chart.draw(data, options);
        }

        if (window.addEventListener) {
            window.addEventListener('resize', resizeHandler, false);
        }
        else if (window.attachEvent) {
            window.attachEvent('onresize', resizeHandler);
        }
    }


    google.charts.setOnLoadCallback(drawChart);

    function drawChart() {

        var data = google.visualization.arrayToDataTable(<?php echo json_encode($countriesData)?>);

        var options = {
            backgroundColor: {fill: '#ffffff'}
        };

        var chart = new google.visualization.PieChart(document.getElementById('pieChart'));

        chart.draw(data, options);

        function resizeHandler() {
            chart.draw(data, options);
        }

        if (window.addEventListener) {
            window.addEventListener('resize', resizeHandler, false);
        }
        else if (window.attachEvent) {
            window.attachEvent('onresize', resizeHandler);
        }
    }
</script>


<?php
include 'footer.php';
?>
