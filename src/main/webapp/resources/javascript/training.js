$(document).ready(function () {

    var remotexmlurl=$( "#xmlresourceurl" ).val() +'/~intact/site/training/training.xml';

   $.ajax({
        type: "GET",
        url: remotexmlurl,
        datatype: "xml",
        crossDomain: true,
        headers: {
            'Access-Control-Allow-Origin': '*'
        },
        error: function (jqXHR, textStatus, errorThrown) {
            console.log('Error: ' + errorThrown);
        },
        success: function (xml) {
            console.log('AJAX Request is succeeded.');

            var f2ftrningrenderer = '';
            var onlinetraining_renderer = '';
            var currentDate = new Date();
            var currentMonth = currentDate.getMonth();
            var currenyYear=currentDate.getFullYear();
            var month = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

            $(xml).find('facetofacetraining').each(function () {

                var monthNumber = parseInt(($(this).find('month').text()));
                var yearNumber = parseInt(($(this).find('year').text()));
                currentMonth+=1;
                if (currenyYear< yearNumber|| ((currenyYear==yearNumber)&&((currentMonth)<=monthNumber))){
                    f2ftrningrenderer += '<h6>';
                    f2ftrningrenderer += month[monthNumber - 1];
                    f2ftrningrenderer += ' ' + $(this).find('year').text();
                    f2ftrningrenderer += '</h6>';
                    f2ftrningrenderer += '<ul>';

                    $(this).find('training').each(function () {

                        var end_training_date_txt = $(this).find('enddate').text();
                        var end_training_date = new Date(end_training_date_txt);

                        if (currentDate.getTime() <= end_training_date.getTime()) {

                            f2ftrningrenderer += '<li><h5>';
                            f2ftrningrenderer += '<a class="external" ';
                            f2ftrningrenderer += 'href="';
                            f2ftrningrenderer += $(this).find('url').text();
                            f2ftrningrenderer += '" ';
                            f2ftrningrenderer += 'target="_blank" ';
                            f2ftrningrenderer += '>';
                            f2ftrningrenderer += $(this).find('name').text();
                            f2ftrningrenderer += '</a>';
                            f2ftrningrenderer += '</h5></li>';
                        }
                    });

                    f2ftrningrenderer += '</ul>';
                }
            });
            $(f2ftrningrenderer).insertAfter("#f2ftraining");

            $(xml).find('onlinetraining').each(function () {
                onlinetraining_renderer += '<section  class="grid_24">';
                onlinetraining_renderer += '<h4>';
                onlinetraining_renderer += $(this).children('name').text();
                onlinetraining_renderer += '</h4>';
                onlinetraining_renderer += '<ul>';
                $(this).find('otraining').each(function () {
                    onlinetraining_renderer += '<li><h5>';
                    onlinetraining_renderer += '<a class="external" ';
                    onlinetraining_renderer += 'href="';
                    onlinetraining_renderer += $(this).find('url').text();
                    onlinetraining_renderer += '" ';
                    onlinetraining_renderer += 'target="_blank" ';
                    onlinetraining_renderer += '>';
                    onlinetraining_renderer += $(this).find('name').text();
                    onlinetraining_renderer += '</a>';

                    onlinetraining_renderer += '</h5></li>';

                });

                onlinetraining_renderer += '</ul>';
                onlinetraining_renderer += '</section>';
            });

            $(onlinetraining_renderer).insertAfter("#onlinetraining");


        }
    });
});
